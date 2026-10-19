package br.com.meutreino.devices

import org.junit.Assert.*
import org.junit.Test

class MiScaleParserTest {
    private fun frame(flags:Int=0x0440, weight:Int=14000): ByteArray {
        val b = ByteArray(13)
        b[0]=(flags and 255).toByte(); b[1]=(flags shr 8).toByte()
        b[2]=0xEA.toByte(); b[3]=0x07 // 2026 little endian
        b[4]=10; b[5]=19
        b[11]=(weight and 255).toByte(); b[12]=(weight shr 8).toByte()
        return b
    }
    @Test fun stableKg() { assertEquals(70.0,MiScaleParser.parse(frame())!!.kilograms,0.0001) }
    @Test fun unstable() { assertNull(MiScaleParser.parse(frame(0x40))) }
    @Test fun emptyScale() { assertNull(MiScaleParser.parse(frame(0x0540))) }
    @Test fun poundsRejected() { assertNull(MiScaleParser.parse(frame(0x04c0))) }
    @Test fun malformed() { assertNull(MiScaleParser.parse(byteArrayOf(1,2))) }
    @Test fun invalidWeight() { assertNull(MiScaleParser.parse(frame(weight=0))) }
}
