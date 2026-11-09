package br.com.meutreino.domain

/** Uma única troca equivalente por aprovação; não muda volume nem duração automaticamente. */
data class ProposedSwap(
    val planId: String,
    val sessionTitle: String,
    val originalId: String,
    val previous: Exercise,
    val replacement: Exercise
)

object ReviewVariationProposal {
    fun candidates(p: UserPrefs): List<ProposedSwap> {
        val env=p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val base=TrainingPlanner.build(p.copy(defaultEnvironment=env))
        val planned=EffectiveTrainingPlan.build(p,env)
        return base.sessions.flatMap { session ->
            val actualSession=planned.sessions.first { it.id == session.id }
            val existingIds=actualSession.exercises.map { it.exercise.id }.toSet()
            session.exercises.mapIndexedNotNull { index, prescribed ->
                val original=prescribed.exercise
                val actual=actualSession.exercises.getOrNull(index)?.exercise ?: original
                val available=ExerciseCatalog.equivalents(original,env,p.equipments,p.experience)
                    .filter { it.id != actual.id && it.id !in existingIds }
                    .sortedWith(compareBy<Exercise> { if(it.primaryMuscle in p.priorityMuscles)0 else 1 }
                        .thenBy { it.minimumLevel }.thenBy { it.id })
                available.firstOrNull()?.let { ProposedSwap(session.id,session.title,original.id,actual,it) }
            }
        }.sortedWith(compareBy<ProposedSwap> { if(it.previous.primaryMuscle in p.priorityMuscles)0 else 1 }
            .thenBy { it.planId }.thenBy { it.originalId }).take(20)
    }
}
