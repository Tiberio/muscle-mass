package br.com.meutreino.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * IAA (Índice de Atividade Acumulada): algoritmo aberto, NÃO É o PAI ou risco clínico.
 * Janela de sete datas locais, incluindo hoje. Recalcula-se desde os eventos originais.
 * 150 minutos moderados = 100 pontos aeróbicos; 75 vigorosos = 100.
 * Musculação concluída com séries: bônus descritivo de 4 pontos/dia (máximo um bônus diário).
 * Atividade leve: registrada no histórico, mas sem pontuação aeróbica nesta versão.
 */
object ActivityScore {
    enum class Kind { CARDIO, STRENGTH }
    enum class Intensity { LIGHT, MODERATE, VIGOROUS, UNKNOWN }
    data class Event(val id: String, val timeMillis: Long, val kind: Kind,
                     val minutes: Int = 0, val intensity: Intensity = Intensity.UNKNOWN,
                     val sets: Int = 0, val source: String = "manual")
    data class Day(val date: LocalDate, val earned: Double, val expired: Double,
                   val balance: Double, val moderateMinutes: Int, val vigorousMinutes: Int,
                   val strength: Boolean)
    data class Summary(val today: LocalDate, val balance: Double, val earnedToday: Double,
                       val expiredToday: Double, val moderateEquivalentMinutes: Int,
                       val strengthDays: Int, val days: List<Day>) {
        val roundedPoints: Int get() = balance.roundToInt()
    }

    fun calculate(events: List<Event>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Summary {
        val unique = events.distinctBy { it.id }.filter { it.minutes >= 0 && it.sets >= 0 }
        val byDay = unique.groupBy { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() }
        fun points(date: LocalDate): Double {
            val a = byDay[date].orEmpty()
            val cardio = a.filter { it.kind == Kind.CARDIO }.sumOf {
                when (it.intensity) {
                    Intensity.MODERATE -> it.minutes * (2.0 / 3.0)
                    Intensity.VIGOROUS -> it.minutes * (4.0 / 3.0)
                    else -> 0.0
                }
            }
            // Um único bônus, por dia, mesmo que haja mais de uma sessão de força.
            val strength = if (a.any { it.kind == Kind.STRENGTH && it.sets > 0 }) 4.0 else 0.0
            return cardio + strength
        }
        val windowStart = today.minusDays(6)
        val days = (0..6).map { offset ->
            val date = windowStart.plusDays(offset.toLong())
            val entries = byDay[date].orEmpty()
            val m = entries.filter { it.kind == Kind.CARDIO && it.intensity == Intensity.MODERATE }.sumOf { it.minutes }
            val v = entries.filter { it.kind == Kind.CARDIO && it.intensity == Intensity.VIGOROUS }.sumOf { it.minutes }
            val earned = points(date)
            // Expiração no dia D: tudo que foi obtido na data D-7 deixa de contar.
            val expired = points(date.minusDays(7))
            Day(date, earned, expired,
                (0..6).sumOf { back -> points(date.minusDays(back.toLong())) },
                m, v, entries.any { it.kind == Kind.STRENGTH && it.sets > 0 })
        }
        val todayRow = days.last()
        return Summary(today, todayRow.balance, todayRow.earned, todayRow.expired,
            days.sumOf { it.moderateMinutes + 2 * it.vigorousMinutes }, days.count { it.strength }, days)
    }

    fun parseIntensity(text: String): Intensity = when (text.uppercase()) {
        "MODERADA", "MODERATE" -> Intensity.MODERATE
        "VIGOROSA", "VIGOROUS" -> Intensity.VIGOROUS
        "LEVE", "LIGHT" -> Intensity.LIGHT
        else -> Intensity.UNKNOWN
    }
}
