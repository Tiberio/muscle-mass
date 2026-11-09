package br.com.meutreino.domain

import br.com.meutreino.data.BodyMeasurementEntity
import br.com.meutreino.data.BodyMeasurementGoalEntity
import br.com.meutreino.data.PlanVersionEntity
import br.com.meutreino.data.RecoveryCheckInEntity
import br.com.meutreino.data.WorkoutSessionEntity
import br.com.meutreino.data.WorkoutSetEntity

/** Converte registros reais do Room em parâmetros objetivos da avaliação. */
object ReviewSnapshotBuilder {
    fun activeVersion(p: UserPrefs, versions: List<PlanVersionEntity>): PlanVersionEntity? {
        val env = p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val hash = PlanVersionSnapshot.fingerprint(PlanVersionSnapshot.content(p, env))
        return versions.firstOrNull { it.fingerprint == hash }
    }

    fun performance(sessions: List<WorkoutSessionEntity>, sets: List<WorkoutSetEntity>): PerformanceTrend {
        val done = sessions.filter { it.status == "COMPLETED" }.associateBy { it.id }
        val complete = sets.filter { it.sessionId in done }
        var comparable = false
        var improved = false
        complete.groupBy { it.exerciseId }.forEach { (_, entries) ->
            val bySession = entries.groupBy { it.sessionId }.mapNotNull { (id, records) ->
                val time = done[id]?.finishedAt ?: return@mapNotNull null
                val best = records.maxWithOrNull(compareBy<WorkoutSetEntity> { it.loadKg ?: -1.0 }
                    .thenBy { it.repetitions }) ?: return@mapNotNull null
                Triple(time, best.loadKg, best.repetitions)
            }.sortedBy { it.first }
            if (bySession.size >= 2) {
                val start = bySession.first()
                val finish = bySession.last()
                val sameLoadKind = (start.second == null) == (finish.second == null)
                if (sameLoadKind && (finish.second ?: 0.0) > (start.second ?: 0.0) + 0.01 ||
                    sameLoadKind && finish.second == start.second && finish.third > start.third) improved = true
                if (bySession.size >= 4 && finish.first - start.first >= 21L * 86_400_000L) comparable = true
            }
        }
        return when { improved -> PerformanceTrend.IMPROVING; comparable -> PerformanceTrend.STABLE; else -> PerformanceTrend.INSUFFICIENT }
    }

    fun prepare(
        p: UserPrefs,
        versions: List<PlanVersionEntity>,
        sessions: List<WorkoutSessionEntity>,
        sets: List<WorkoutSetEntity>,
        checkins: List<RecoveryCheckInEntity>,
        goals: List<BodyMeasurementGoalEntity>,
        measurements: List<BodyMeasurementEntity>,
        reason: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<PlanVersionEntity, ReviewReport>? {
        val current = activeVersion(p, versions) ?: return null
        val done = sessions.filter { it.planVersionId == current.id && it.status == "COMPLETED" }
        val doneIds = done.map { it.id }.toSet()
        val completedSets = sets.filter { it.sessionId in doneIds }
        // Sessão efetiva de força: concluída e com pelo menos uma série real gravada.
        val effectiveCount = completedSets.map { it.sessionId }.toSet().size
        val exposures = done.flatMap { session ->
            completedSets.filter { it.sessionId == session.id }
                .mapNotNull { set -> ExerciseCatalog.byId(set.exerciseId)?.primaryMuscle }
                .distinct()
        }.groupingBy { it }.eachCount()
        val env = p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val activeMuscles = EffectiveTrainingPlan.build(p,env).sessions
            .flatMap { it.exercises.map { item -> item.exercise.primaryMuscle } }.toSet()
        val relevant = if (p.priorityMuscles.any { it in activeMuscles }) p.priorityMuscles.filter { it in activeMuscles }.toSet()
                       else activeMuscles
        val latestCheck = checkins.firstOrNull { it.planVersionId == current.id &&
            it.recordedAt <= nowMillis && nowMillis-it.recordedAt <= 14L*86_400_000L }
        val recovery = when(latestCheck?.state) {
            "BEM" -> RecoveryFlag.WELL
            "CANSADO" -> RecoveryFlag.TIRED
            "DOR_PERSISTENTE" -> RecoveryFlag.PERSISTENT_PAIN
            else -> RecoveryFlag.UNKNOWN
        }
        val goalsProgress = goals.mapNotNull { goal ->
            val actual = measurements.filter { it.site == goal.site && it.recordedAt >= goal.startedAt }
                .maxByOrNull { it.recordedAt } ?: return@mapNotNull null
            ReviewEngine.goalProgress(goal.site,goal.initialCm,goal.targetCm,actual.centimeters)
        }
        val targetDays = p.reviewWindowDays.coerceIn(28,112)
        val sessionsPerWeek = TrainingPlanner.build(p).sessions.size.coerceIn(1,7)
        val sessionGoal = (sessionsPerWeek * targetDays / 7).coerceAtLeast(1)
        val input = ReviewInput(nowMillis,current.createdAt,effectiveCount,exposures,relevant,
            performance(done,completedSets),recovery,goalsProgress,reason,
            ReviewParameters(targetDays,sessionGoal,p.reviewGroupExposures.coerceIn(6,20),p.reviewEarlyPercent.coerceIn(60,100)/100.0))
        return current to ReviewEngine.analyze(input)
    }
}
