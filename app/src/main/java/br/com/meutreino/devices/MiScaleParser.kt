package br.com.meutreino.devices

/** Interpreta os 13 bytes de Service Data (UUID 0x181B) da Mi Body Composition Scale 2.
 *  Fontes do protocolo: freenetwork/ble-in-xiaomi e oliexdev/openScale.
 *  Só aceita medições estabilizadas em quilogramas; impedância não é estimativa clínica.
 */
object MiScaleParser {
    data class Measurement(val kilograms: Double, val stable: Boolean, val hasImpedance: Boolean)

    fun parse(payload: ByteArray): Measurement? {
        if (payload.size < 13) return null
        val flags = u16(payload, 0)
        val emptyLoad = flags and (1 shl 8) != 0
        val stable = flags and (1 shl 10) != 0
        val pounds = flags and (1 shl 7) != 0
        val catty = flags and (1 shl 9) != 0
        if (emptyLoad || !stable || pounds || catty) return null
        // Filtro de integridade simples: são anúncios de medições datadas.
        val year = u16(payload, 2)
        val month = payload[4].toInt() and 0xff
        val day = payload[5].toInt() and 0xff
        if (year !in 2016..2100 || month !in 1..12 || day !in 1..31) return null
        val kg = u16(payload, 11) / 200.0
        if (kg !in 2.0..350.0) return null
        return Measurement(kg, true, flags and (1 shl 14) != 0)
    }
    private fun u16(b: ByteArray, n: Int) = (b[n].toInt() and 0xff) or ((b[n + 1].toInt() and 0xff) shl 8)
}
