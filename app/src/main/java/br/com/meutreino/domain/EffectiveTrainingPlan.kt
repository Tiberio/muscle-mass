package br.com.meutreino.domain

/** Aplica substituições aprovadas sem alterar a seleção basal, mantendo IDs reais nas sessões. */
object EffectiveTrainingPlan {
    fun build(p: UserPrefs, environment: String): TrainingPlan {
        val base = TrainingPlanner.build(p.copy(defaultEnvironment = environment))
        return base.copy(sessions = base.sessions.map { session ->
            val original = session.exercises
            session.copy(exercises = original.map { prescribed ->
                val ex = prescribed.exercise
                val requested = ExerciseCatalog.byId(p.overrides["${session.id}:${ex.id}"] ?: ex.id)
                val valid = requested != null && requested in ExerciseCatalog.equivalents(ex,environment,p.equipments,p.experience)
                prescribed.copy(exercise = if (valid) requested!! else ex)
            })
        })
    }
}
