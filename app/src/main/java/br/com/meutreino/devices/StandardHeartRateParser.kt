package br.com.meutreino.devices

/** Decodifica SOMENTE a característica padronizada Bluetooth 0x2A37, quando disponível. */
object StandardHeartRateParser {
    fun parse(bytes: ByteArray): Int? {
        if (bytes.size < 2) return null
        val flags = bytes[0].toInt() and 0xff
        val is16Bit = flags and 0x01 != 0
        val bpm = if (is16Bit) {
            if (bytes.size < 3) return null
            (bytes[1].toInt() and 0xff) or ((bytes[2].toInt() and 0xff) shl 8)
        } else bytes[1].toInt() and 0xff
        return bpm.takeIf { it in 20..250 }
    }
}

