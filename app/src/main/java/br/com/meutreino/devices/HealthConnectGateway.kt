package br.com.meutreino.devices

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import br.com.meutreino.domain.HealthIaaBridge
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.reflect.KClass

/** Dados do Health Connect são históricos, não streaming da Mi Band. */
data class HealthDaySteps(val date: LocalDate, val steps: Long)
data class HealthExercise(val title: String, val startedAt: Instant, val minutes: Long, val source: String?,
                          val endedAt: Instant = startedAt, val recordId: String = "")
data class HeartDay(val date: LocalDate, val average: Int, val minimum: Int, val maximum: Int, val samples: Int)
data class SleepDay(val date: LocalDate, val minutes: Long)
data class HealthSnapshot(
    val stepsToday: Long?, val stepsLast7Days: Long?,
    val latestWeightKg: Double?, val latestWeightAt: Instant?, val latestWeightSource: String?,
    val latestHeartBpm: Long?, val latestHeartAt: Instant?, val latestHeartSource: String?,
    val exerciseSessionsLast7Days: Int?, val updatedAt: Instant,
    val stepsByDay: List<HealthDaySteps> = emptyList(),
    val recentExercise: List<HealthExercise> = emptyList(),
    val lastSleepMinutes: Long? = null, val lastSleepAt: Instant? = null,
    val lastSleepSource: String? = null, val sleepPermission: Boolean = false,
    val heartByDay: List<HeartDay> = emptyList(),
    val sleepByDay: List<SleepDay> = emptyList(),
    val allExercise: List<HealthExercise> = emptyList(),
    val heartSamples: List<HealthIaaBridge.Heart> = emptyList(),
    val heartPermission: Boolean = false, val exercisePermission: Boolean = false,
    val truncated: Boolean = false
)

class HealthConnectGateway(private val context: Context) {
    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
        fun status(context: Context): Int = HealthConnectClient.getSdkStatus(context)
        fun available(context: Context): Boolean = status(context) == HealthConnectClient.SDK_AVAILABLE
        fun permissionsContract() = PermissionController.createRequestPermissionResultContract()
    }

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)
    suspend fun grantedPermissions(): Set<String> = client().permissionController.getGrantedPermissions()

    suspend fun readSnapshot(): HealthSnapshot {
        val hc = client()
        val granted = hc.permissionController.getGrantedPermissions()
        require(granted.any { it in permissions }) { "Nenhuma permissão de leitura concedida." }
        val canSteps = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val canWeight = HealthPermission.getReadPermission(WeightRecord::class) in granted
        val canHeart = HealthPermission.getReadPermission(HeartRateRecord::class) in granted
        val canExercise = HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted
        val canSleep = HealthPermission.getReadPermission(SleepSessionRecord::class) in granted
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = Instant.now()
        val midnight = today.atStartOfDay(zone).toInstant()
        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant()
        var truncated = false

        // Ler todas as páginas; limitar consultas volumosas para proteger memória/bateria.
        suspend fun <T : Record> readAll(type: KClass<T>, start: Instant, end: Instant,
                                         limit: Int = 5000): List<T> {
            val result = mutableListOf<T>()
            var token: String? = null
            var pages = 0
            do {
                val response = hc.readRecords(ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageSize = 250,
                    pageToken = token
                ))
                result.addAll(response.records)
                token = response.pageToken
                pages++
                if (!token.isNullOrEmpty() && (result.size >= limit || pages >= 40)) {
                    truncated = true
                    break
                }
            } while (!token.isNullOrEmpty())
            return result.take(limit)
        }

        // O agregador do Health Connect aplica as regras de priorização das fontes.
        suspend fun countSteps(start: Instant, end: Instant): Long =
            hc.aggregate(AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            ))[StepsRecord.COUNT_TOTAL] ?: 0L

        val weekSteps = if (canSteps) countSteps(weekStart, now) else null
        val todaySteps = if (canSteps) countSteps(midnight, now) else null
        val dailySteps = if (canSteps) (6 downTo 0).map { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            val start = day.atStartOfDay(zone).toInstant()
            val end = minOf(day.plusDays(1).atStartOfDay(zone).toInstant(), now)
            HealthDaySteps(day, countSteps(start, end))
        } else emptyList()

        val weight = if (canWeight) hc.readRecords(ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(today.minusDays(29).atStartOfDay(zone).toInstant(), now),
            ascendingOrder = false, pageSize = 1
        )).records.firstOrNull() else null

        val hrRecords = if (canHeart) readAll(HeartRateRecord::class, weekStart, now) else emptyList()
        // Um minuto por origem basta para gráfico e avaliação conservadora de cobertura.
        val heart = hrRecords.flatMap { record ->
            record.samples.map { sample -> HealthIaaBridge.Heart(
                sample.time.toEpochMilli(), sample.beatsPerMinute,
                record.metadata.dataOrigin.packageName
            ) }
        }.filter { it.bpm in 30..240 }
            .groupBy { (it.timeMillis / 60_000L) to it.source }
            .values.map { points -> points.maxByOrNull { it.timeMillis }!! }
            .sortedBy { it.timeMillis }
        val latestHeart = heart.lastOrNull()
        val dailyHeart = heart.groupBy { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() }
            .map { (day, points) -> HeartDay(day,
                points.map { it.bpm }.average().roundToInt(),
                points.minOf { it.bpm }.toInt(),
                points.maxOf { it.bpm }.toInt(), points.size)
            }.sortedBy { it.date }

        val exercises = if (canExercise) readAll(ExerciseSessionRecord::class, weekStart, now, limit=2000)
            .map { record -> HealthExercise(
                record.title?.takeIf { it.isNotBlank() } ?: "Sessão de exercício",
                record.startTime, Duration.between(record.startTime, record.endTime).toMinutes().coerceAtLeast(0),
                record.metadata.dataOrigin.packageName, record.endTime, record.metadata.id
            ) }.sortedByDescending { it.startedAt } else emptyList()

        val sleeps = if (canSleep) readAll(SleepSessionRecord::class, weekStart, now, limit=1000)
            .sortedByDescending { it.endTime } else emptyList()
        val dailySleep = sleeps.groupBy { it.endTime.atZone(zone).toLocalDate() }
            .map { (day, records) -> SleepDay(day, records.sumOf {
                Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0)
            }) }.sortedBy { it.date }
        val lastSleep = sleeps.firstOrNull()

        return HealthSnapshot(
            stepsToday=todaySteps, stepsLast7Days=weekSteps,
            latestWeightKg=weight?.weight?.inKilograms, latestWeightAt=weight?.time,
            latestWeightSource=weight?.metadata?.dataOrigin?.packageName,
            latestHeartBpm=latestHeart?.bpm, latestHeartAt=latestHeart?.let { Instant.ofEpochMilli(it.timeMillis) },
            latestHeartSource=latestHeart?.source,
            exerciseSessionsLast7Days=if (canExercise) exercises.size else null,
            // Registrar o fim da consulta concluída, não o início da coleta.
            updatedAt=Instant.now(), stepsByDay=dailySteps, recentExercise=exercises.take(6),
            lastSleepMinutes=lastSleep?.let { Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0) },
            lastSleepAt=lastSleep?.endTime, lastSleepSource=lastSleep?.metadata?.dataOrigin?.packageName,
            sleepPermission=canSleep, heartByDay=dailyHeart, sleepByDay=dailySleep,
            allExercise=exercises, heartSamples=heart,
            heartPermission=canHeart, exercisePermission=canExercise, truncated=truncated
        )
    }
}
