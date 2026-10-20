package br.com.meutreino.domain

import kotlin.math.ceil
import kotlin.math.floor

/** Escalas numéricas para os gráficos do painel, sem dependências do Android.
 * Apenas medições existentes definem a escala; valores ausentes não são interpolados.
 */
object ChartAxis {
    enum class Unit { BPM, HOURS, POINTS }

    data class Scale(val min: Double, val max: Double, val ticks: List<Double>)

    /** Retorna cinco marcas igualmente espaçadas e limites que contêm todas as medições válidas. */
    fun calculate(values: List<Double?>, unit: Unit): Scale? {
        val valid = values.filterNotNull().filter { it.isFinite() }
        if (valid.isEmpty()) return null
        val lowest = valid.minOrNull()!!
        val highest = valid.maxOrNull()!!
        val quantum = when (unit) {
            Unit.BPM -> 5.0
            Unit.HOURS -> 1.0
            Unit.POINTS -> 5.0
        }
        val minimum = when (unit) {
            Unit.BPM -> floor(lowest / quantum) * quantum
            Unit.HOURS -> minOf(0.0, floor(lowest))
            Unit.POINTS -> minOf(0.0, floor(lowest / quantum) * quantum)
        }
        val steps = ceil((highest - minimum) / (4.0 * quantum)).toInt().coerceAtLeast(1)
        val increment = quantum * steps
        val ticks = (0..4).map { minimum + it * increment }
        return Scale(minimum, ticks.last(), ticks)
    }

    fun label(value: Double): String = value.toLong().toString()
}
