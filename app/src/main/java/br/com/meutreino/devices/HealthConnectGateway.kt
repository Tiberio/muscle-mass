package br.com.meutreino.devices

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Dados mostrados sob demanda; não entram no IAA para impedir contagem duplicada. */
data class HealthSnapshot(
    val stepsToday: Long?,
    val stepsLast7Days: Long?,
    val latestWeightKg: Double?,
    val latestWeightAt: Instant?,
    val latestWeightSource: String?,
    val latestHeartBpm: Long?,
    val latestHeartAt: Instant?,
    val latestHeartSource: String?,
    val exerciseSessionsLast7Days: Int?,
    val updatedAt: Instant
)

class HealthConnectGateway(private val context: Context) {
    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
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
        fun allowedStep() = HealthPermission.getReadPermission(StepsRecord::class) in granted
        fun allowedWeight() = HealthPermission.getReadPermission(WeightRecord::class) in granted
        fun allowedHeart() = HealthPermission.getReadPermission(HeartRateRecord::class) in granted
        fun allowedExercise() = HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = Instant.now()
        val midnight = today.atStartOfDay(zone).toInstant()
        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant()
        val steps = if(allowedStep()) hc.aggregate(AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(weekStart, now)
        ))[StepsRecord.COUNT_TOTAL] ?: 0L else null
        val todaySteps = if(allowedStep()) hc.aggregate(AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(midnight, now)
        ))[StepsRecord.COUNT_TOTAL] ?: 0L else null
        // O último registro tem prioridade; a fonte não é atribuída arbitrariamente ao relógio.
        val weight = if(allowedWeight()) hc.readRecords(ReadRecordsRequest(
            WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(today.minusDays(29).atStartOfDay(zone).toInstant(), now),
            ascendingOrder = false, pageSize = 1
        )).records.firstOrNull() else null
        // Leitura pontual das últimas 24 horas, sem monitoramento contínuo.
        val heartRecords = if(allowedHeart()) hc.readRecords(ReadRecordsRequest(
            HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(now.minusSeconds(24L * 60L * 60L), now),
            ascendingOrder = false, pageSize = 100
        )).records else emptyList()
        val lastHeartRecord = heartRecords.maxByOrNull { it.endTime }
        val hr = heartRecords.flatMap { it.samples }.maxByOrNull { it.time }
        val workouts = if(allowedExercise()) hc.readRecords(ReadRecordsRequest(
            ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(weekStart, now),
            ascendingOrder = false, pageSize = 1000
        )).records.size else null
        return HealthSnapshot(
            todaySteps, steps, weight?.weight?.inKilograms, weight?.time,
            weight?.metadata?.dataOrigin?.packageName,
            hr?.beatsPerMinute, hr?.time, lastHeartRecord?.metadata?.dataOrigin?.packageName,
            workouts, now
        )
    }
}
