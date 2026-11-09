package br.com.meutreino.tests

import br.com.meutreino.domain.*

fun main() {
    val config = ReviewParameters(56,24,10,0.8)
    val day = 86_400_000L
    val default = ReviewInput(45*day,0,4,mapOf("Peito" to 2),setOf("Peito"),PerformanceTrend.INSUFFICIENT,
        parameters=config)
    val a=ReviewEngine.analyze(default)
    check(a.eligibleForEarlyReview && a.verdict==ReviewVerdict.REVIEW)
    check(ReviewEngine.pct(a.timeFraction)=="80%")
    val b=ReviewEngine.analyze(default.copy(nowMillis=20*day,completedEffectiveSessions=20))
    check(b.eligibleForEarlyReview && b.verdict==ReviewVerdict.REVIEW)
    val c=ReviewEngine.analyze(default.copy(nowMillis=21*day,completedEffectiveSessions=2,
        groupExposures=mapOf("Peito" to 8)))
    check(c.eligibleForEarlyReview && c.verdict==ReviewVerdict.REVIEW)
    val d=ReviewEngine.analyze(default.copy(nowMillis=10*day,completedEffectiveSessions=2,
        groupExposures=mapOf("Peito" to 1)))
    check(d.verdict==ReviewVerdict.MAINTAIN && d.requiresExtraConfirmation)
    val e=ReviewEngine.analyze(default.copy(nowMillis=56*day,completedEffectiveSessions=25,
        performance=PerformanceTrend.IMPROVING))
    check(e.verdict==ReviewVerdict.REVIEW && e.requiresExtraConfirmation)
    val f=ReviewEngine.analyze(default.copy(nowMillis=56*day,completedEffectiveSessions=25,
        performance=PerformanceTrend.STABLE,reason="ESTAGNACAO",recovery=RecoveryFlag.WELL))
    check(f.verdict==ReviewVerdict.VARIATION_POSSIBLE && !f.requiresExtraConfirmation)
    val g=ReviewEngine.analyze(default.copy(recovery=RecoveryFlag.PERSISTENT_PAIN))
    check(g.verdict==ReviewVerdict.CAUTION)
    val goalUp=ReviewEngine.goalProgress("Braço",32.0,34.0,33.6)!!
    val goalDown=ReviewEngine.goalProgress("Cintura",86.0,82.0,82.8)!!
    check(kotlin.math.abs(goalUp.fraction-0.8)<1e-9)
    check(kotlin.math.abs(goalDown.fraction-0.8)<1e-9)
    check(ReviewEngine.goalProgress("X",32.0,32.0,32.0)==null)
    val prefs=UserPrefs(defaultEnvironment="Academia",weekdays=listOf("Seg","Qua","Sex"))
    val swaps=ReviewVariationProposal.candidates(prefs)
    check(swaps.all { it.previous.id!=it.replacement.id })
    check(swaps.all { it.previous.pattern==it.replacement.pattern && it.previous.primaryMuscle==it.replacement.primaryMuscle })
    swaps.take(4).forEach { swap ->
        val proposed=prefs.copy(overrides=mapOf("${swap.planId}:${swap.originalId}" to swap.replacement.id))
        val first=PlanVersionSnapshot.content(prefs,"Academia")
        val second=PlanVersionSnapshot.content(proposed,"Academia")
        check(PlanVersionSnapshot.fingerprint(first)!=PlanVersionSnapshot.fingerprint(second))
        check(second.contains(swap.replacement.id))
    }
    println("REVIEW ENGINE OK: 80% (dias/sessões/grupos), metas ascendentes/descendentes, cautela, confirmações, equivalências e versões")
}
