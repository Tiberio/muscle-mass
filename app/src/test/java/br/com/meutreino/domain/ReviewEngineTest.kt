package br.com.meutreino.domain

import org.junit.Assert.*
import org.junit.Test

class ReviewEngineTest {
    private val day=86_400_000L
    private val params=ReviewParameters(56,24,10,0.8)
    private fun input(days:Long=14,sessions:Int=3,groups:Map<String,Int> = mapOf("Peito" to 2),
                      trend:PerformanceTrend=PerformanceTrend.INSUFFICIENT,reason:String="VARIEDADE") =
        ReviewInput(days*day,0,sessions,groups,setOf("Peito"),trend,reason=reason,parameters=params)

    @Test fun eightyPercentTimeTriggersReviewButNotAutomaticChange() {
        val report=ReviewEngine.analyze(input(days=45))
        assertTrue(report.eligibleForEarlyReview)
        assertEquals(ReviewVerdict.REVIEW,report.verdict)
        assertTrue(report.requiresExtraConfirmation)
    }
    @Test fun eightyPercentSessionsTriggersEarlierThanCalendar() {
        assertTrue(ReviewEngine.analyze(input(days=12,sessions=20)).eligibleForEarlyReview)
    }
    @Test fun eightyPercentGroupExposuresTriggersEarlierThanCalendar() {
        assertTrue(ReviewEngine.analyze(input(days=12,groups=mapOf("Peito" to 8))).eligibleForEarlyReview)
    }
    @Test fun progressMeansNoAutomaticVariationEvenAtFullWindow() {
        val report=ReviewEngine.analyze(input(days=56,sessions=25,trend=PerformanceTrend.IMPROVING,reason="ESTAGNACAO"))
        assertEquals(ReviewVerdict.REVIEW,report.verdict)
        assertTrue(report.requiresExtraConfirmation)
    }
    @Test fun onlyMatchingPlateauAndUserRequestSuggestsVariation() {
        val report=ReviewEngine.analyze(input(days=56,sessions=25,trend=PerformanceTrend.STABLE,reason="ESTAGNACAO"))
        assertEquals(ReviewVerdict.VARIATION_POSSIBLE,report.verdict)
    }
    @Test fun persistentPainPreventsAutomaticProposal() {
        val report=ReviewEngine.analyze(input(days=56,sessions=25).copy(recovery=RecoveryFlag.PERSISTENT_PAIN))
        assertEquals(ReviewVerdict.CAUTION,report.verdict)
    }
    @Test fun goalProgressCorrectForIncreaseAndDecrease() {
        assertEquals(0.8,ReviewEngine.goalProgress("braço",32.0,34.0,33.6)!!.fraction,1e-7)
        assertEquals(0.8,ReviewEngine.goalProgress("cintura",86.0,82.0,82.8)!!.fraction,1e-7)
        assertNull(ReviewEngine.goalProgress("braço",33.0,33.0,33.0))
    }
    @Test fun draftDoesNotChangePlanAndProposalsAreEquivalent() {
        val p=UserPrefs(defaultEnvironment="Academia",weekdays=listOf("Seg","Qua","Sex"))
        val base=PlanVersionSnapshot.content(p,"Academia")
        val swaps=ReviewVariationProposal.candidates(p)
        assertTrue(swaps.isNotEmpty())
        val choice=swaps.first()
        assertEquals(choice.previous.pattern,choice.replacement.pattern)
        assertEquals(choice.previous.primaryMuscle,choice.replacement.primaryMuscle)
        val newP=p.copy(overrides=mapOf("${choice.planId}:${choice.originalId}" to choice.replacement.id))
        assertNotEquals(PlanVersionSnapshot.fingerprint(base),PlanVersionSnapshot.fingerprint(PlanVersionSnapshot.content(newP,"Academia")))
        assertEquals(base,PlanVersionSnapshot.content(p,"Academia"))
    }
}
