package br.com.meutreino.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StandardHeartRateParserTest {
    @Test fun eightBitHeartRate() = assertEquals(76, StandardHeartRateParser.parse(byteArrayOf(0,76)))
    @Test fun sixteenBitHeartRate() = assertEquals(120, StandardHeartRateParser.parse(byteArrayOf(1,120,0)))
    @Test fun rejectsTruncatedAndInvalid() {
        assertNull(StandardHeartRateParser.parse(byteArrayOf()))
        assertNull(StandardHeartRateParser.parse(byteArrayOf(1,120)))
        assertNull(StandardHeartRateParser.parse(byteArrayOf(0,0)))
        assertNull(StandardHeartRateParser.parse(byteArrayOf(0,(-1).toByte())))
    }
}
