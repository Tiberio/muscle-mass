package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class ChartAxisTest {
    @Test fun bpmLabelsCoverMeasurements() {
        val scale = ChartAxis.calculate(listOf(58.0, 76.5, null, 112.0), ChartAxis.Unit.BPM)!!
        assertEquals(5, scale.ticks.size)
        assertTrue(scale.min <= 58.0)
        assertTrue(scale.max >= 112.0)
        assertTrue(scale.ticks.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun sleepHasZeroAndHourLabels() {
        val scale = ChartAxis.calculate(listOf(7.25, null, 8.0), ChartAxis.Unit.HOURS)!!
        assertEquals(0.0, scale.min, 0.0)
        assertTrue(scale.max >= 8.0)
        assertEquals("2", ChartAxis.label(scale.ticks[1]))
    }

    @Test fun iaaScaleIncludesZeroAndAllPoints() {
        val scale = ChartAxis.calculate(listOf(21.5, 107.5), ChartAxis.Unit.POINTS)!!
        assertEquals(0.0, scale.min, 0.0)
        assertTrue(scale.max >= 107.5)
    }

    @Test fun emptySeriesDoesNotInventValues() {
        assertNull(ChartAxis.calculate(listOf(null, Double.NaN), ChartAxis.Unit.BPM))
    }

    @Test fun constantValueStillGetsReadableScale() {
        val scale = ChartAxis.calculate(listOf(76.0, 76.0), ChartAxis.Unit.BPM)!!
        assertTrue(scale.max > scale.min)
        assertTrue(scale.ticks.contains(76.0) || (76.0 in scale.min..scale.max))
    }
}
