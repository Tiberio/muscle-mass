import br.com.meutreino.domain.ChartAxis

fun main() {
    for (unit in ChartAxis.Unit.entries) {
        val data = when (unit) {
            ChartAxis.Unit.BPM -> listOf(60.0, 80.0, 115.0)
            ChartAxis.Unit.HOURS -> listOf(6.2, 7.4, 8.1)
            ChartAxis.Unit.POINTS -> listOf(0.0, 32.0, 107.5)
        }
        val axis = ChartAxis.calculate(data, unit)!!
        check(axis.ticks.size == 5)
        check(axis.min <= data.min())
        check(axis.max >= data.max())
        check(axis.ticks.zipWithNext().all { (a, b) -> b > a })
        println("${unit.name}: ${axis.ticks.joinToString { ChartAxis.label(it) }}")
    }
    check(ChartAxis.calculate(listOf(null, null), ChartAxis.Unit.BPM) == null)
}
