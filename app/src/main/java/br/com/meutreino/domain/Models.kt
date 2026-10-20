package br.com.meutreino.domain

data class UserPrefs(
    val age: String = "", val heightCm: String = "", val weightKg: String = "",
    val experience: String = "Iniciante", val regularity: String = "Retornando / começando",
    val limitations: String = "", val objective: String = "Saúde e bem-estar",
    val secondaryGoals: List<String> = emptyList(),
    val priorityMuscles: List<String> = emptyList(),
    val environments: List<String> = emptyList(),
    val defaultEnvironment: String = "",
    val equipments: List<String> = emptyList(),
    val scheduleMode: String = "Dias definidos",
    val weekdays: List<String> = emptyList(),
    val weeklyCount: Int = 3, val minutesPerSession: Int = 45,
    val preferredTime: String = "Sem preferência", val flexibleDuration: Boolean = false,
    val cardioActivities: List<String> = emptyList(),
    val cardioWhen: String = "Misto", val cardioMinutes: Int = 15,
    val cardioWeeklyDays: Int = 0, // 0 = junto aos dias de musculação
    val cardioIntensity: String = "Adaptativa",
    val deviceNames: List<String> = emptyList(),
    val deviceApp: String = "Zepp Life", val importHistory: Boolean = false,
    val overrides: Map<String, String> = emptyMap(),
    val assignments: Map<String, String> = emptyMap(),
    val onboardingCompleted: Boolean = false,
    val planAccepted: Boolean = false
)

data class Exercise(
    val id: String,
    val name: String,
    val pattern: String,
    val primaryMuscle: String,
    val secondaryMuscles: Set<String> = emptySet(),
    val required: Set<String> = emptySet(),
    val minimumLevel: Int = 0,
    val advice: String,
    val youtubeVideoId: String? = null,
    val modality: String = "Musculação",
    val commonMistakes: String = "",
    val regressionId: String? = null,
    val progressionId: String? = null,
    val safetyNote: String = ""
)

data class PlannedExercise(val exercise: Exercise, val sets: Int, val minReps: Int, val maxReps: Int)
data class PlannedSession(
    val id: String, val title: String, val day: String,
    val minutes: Int, val exercises: List<PlannedExercise>,
    val explanation: String, val warning: String? = null
)

data class TrainingPlan(val sessions: List<PlannedSession>, val explanation: String, val warnings: List<String>)
