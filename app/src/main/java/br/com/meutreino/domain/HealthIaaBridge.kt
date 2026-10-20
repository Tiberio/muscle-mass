package br.com.meutreino.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Regras abertas de inclusão do Health Connect no IAA, sem alegar equivalência ao PAI.
 * Sem dados de frequência cardíaca suficientes, não cria pontos hipotéticos.
 */
object HealthIaaBridge {
    data class Heart(val timeMillis: Long, val bpm: Long, val source: String)
    data class Session(val id: String, val startMillis: Long, val endMillis: Long,
                       val title: String, val source: String)
    enum class Status { INCLUDED, MISSING_AGE, INSUFFICIENT_HEART, DUPLICATE_LOCAL, DUPLICATE_HEALTH, INVALID_DURATION }
    data class Assessment(val session: Session, val status: Status, val coveragePercent: Int,
                          val moderateMinutes: Int, val vigorousMinutes: Int) {
        val estimatedPoints: Double get() = moderateMinutes * (2.0 / 3.0) + vigorousMinutes * (4.0 / 3.0)
    }

    fun assess(sessions: List<Session>, samples: List<Heart>, age: Int?,
               localCardioDates: Set<LocalDate>, zone: ZoneId = ZoneId.systemDefault()): List<Assessment> {
        val maxHr = age?.takeIf { it in 18..100 }?.let { 220 - it }
        val sorted = sessions.sortedWith(compareBy<Session> { it.startMillis }.thenBy { it.endMillis })
        val occupied = mutableListOf<Session>()
        val validSamples = samples.filter { it.bpm in 30..240 }.distinctBy { it.timeMillis to it.source }
        return sorted.map { session ->
            val duration = session.endMillis - session.startMillis
            val day = Instant.ofEpochMilli(session.startMillis).atZone(zone).toLocalDate()
            val match = occupied.any { other ->
                val overlap = (minOf(session.endMillis, other.endMillis) - maxOf(session.startMillis, other.startMillis)).coerceAtLeast(0)
                overlap >= minOf(duration, other.endMillis - other.startMillis) / 2
            }
            val status = when {
                duration < 5 * 60_000L || duration > 6 * 3600_000L -> Status.INVALID_DURATION
                day in localCardioDates -> Status.DUPLICATE_LOCAL
                match -> Status.DUPLICATE_HEALTH
                maxHr == null -> Status.MISSING_AGE
                else -> null
            }
            if (status == null) occupied.add(session)
            val perMinute = if (status == null) validSamples.asSequence()
                .filter { it.timeMillis in session.startMillis until session.endMillis }
                .groupBy { (it.timeMillis - session.startMillis) / 60_000L }
                .mapValues { (_, points) -> points.map { it.bpm }.average() }
            else emptyMap()
            val totalMinutes = (duration / 60_000L).toInt().coerceAtLeast(1)
            val coverage = ((perMinute.size * 100.0) / totalMinutes).roundToInt().coerceIn(0, 100)
            val included = status == null && coverage >= 60 && perMinute.size >= 5
            val moderate = if (included) perMinute.values.count { it >= maxHr!! * .50 && it < maxHr * .70 } else 0
            val vigorous = if (included) perMinute.values.count { it >= maxHr!! * .70 } else 0
            Assessment(session, status ?: if(included) Status.INCLUDED else Status.INSUFFICIENT_HEART,
                coverage, moderate, vigorous)
        }
    }

    /** Cada evento tem ID estável; nenhuma importação persiste uma cópia no banco local. */
    fun events(assessments: List<Assessment>): List<ActivityScore.Event> = assessments.flatMap { a ->
        if (a.status != Status.INCLUDED) emptyList() else buildList {
            if (a.moderateMinutes > 0) add(ActivityScore.Event("hc:${a.session.id}:moderate",a.session.startMillis,
                ActivityScore.Kind.CARDIO,a.moderateMinutes,ActivityScore.Intensity.MODERATE,source="Health Connect: ${a.session.source}"))
            if (a.vigorousMinutes > 0) add(ActivityScore.Event("hc:${a.session.id}:vigorous",a.session.startMillis,
                ActivityScore.Kind.CARDIO,a.vigorousMinutes,ActivityScore.Intensity.VIGOROUS,source="Health Connect: ${a.session.source}"))
        }
    }
}
