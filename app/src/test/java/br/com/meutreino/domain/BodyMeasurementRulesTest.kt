package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class BodyMeasurementRulesTest {
    @Test fun aceitaVirgulaDecimal() {
        assertEquals(32.5,BodyMeasurementRules.parse("32,5") ?: 0.0,0.001)
        assertTrue(BodyMeasurementRules.valid(32.5))
    }
    @Test fun recusaMedidasInvalidas() {
        assertFalse(BodyMeasurementRules.valid(Double.NaN))
        assertFalse(BodyMeasurementRules.valid(Double.POSITIVE_INFINITY))
        assertFalse(BodyMeasurementRules.valid(-1.0))
        assertNull(BodyMeasurementRules.parse("não é número"))
    }
}
