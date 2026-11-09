package br.com.meutreino.domain

/** Localização e padronização para registros de circunferências (não diagnósticos). */
object BodyMeasurementRules {
    val sites = listOf(
        "Bíceps direito (braço contraído)",
        "Bíceps esquerdo (braço contraído)",
        "Tríceps direito (braço relaxado)",
        "Tríceps esquerdo (braço relaxado)",
        "Peitoral", "Abdômen", "Coxa direita", "Coxa esquerda",
        "Panturrilha direita", "Panturrilha esquerda", "Pescoço"
    )
    fun parse(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
    fun valid(value: Double): Boolean = value.isFinite() && value in 5.0..250.0
}
