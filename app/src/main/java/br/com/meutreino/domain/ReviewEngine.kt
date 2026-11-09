package br.com.meutreino.domain

import kotlin.math.roundToInt

/** Valores experimentais de produto, NÃO prazos determinados por entidades médicas. */
data class ReviewParameters(
    val windowDays: Int = 56,
    val targetSessions: Int = 24,
    val targetExposuresPerGroup: Int = 10,
    val earlyReviewFraction: Double = 0.80
) {
    init {
        require(windowDays in 14..365 && targetSessions in 1..365)
        require(targetExposuresPerGroup in 1..120 && earlyReviewFraction in 0.5..1.0)
    }
}

enum class PerformanceTrend { IMPROVING, STABLE, INSUFFICIENT }
enum class RecoveryFlag { WELL, TIRED, PERSISTENT_PAIN, UNKNOWN }
enum class ReviewVerdict { MAINTAIN, REVIEW, VARIATION_POSSIBLE, CAUTION }

data class BodyGoalProgress(
    val site: String,
    val initialCm: Double,
    val targetCm: Double,
    val currentCm: Double,
    val fraction: Double
)

data class ReviewInput(
    val nowMillis: Long,
    val activeSinceMillis: Long,
    val completedEffectiveSessions: Int,
    val groupExposures: Map<String, Int>,
    val relevantGroups: Set<String>,
    val performance: PerformanceTrend,
    val recovery: RecoveryFlag = RecoveryFlag.UNKNOWN,
    val goals: List<BodyGoalProgress> = emptyList(),
    val reason: String = "VARIEDADE",
    val parameters: ReviewParameters = ReviewParameters()
)

data class ReviewReport(
    val elapsedDays: Long,
    val timeFraction: Double,
    val sessionsFraction: Double,
    val exposureFraction: Map<String, Double>,
    val eligibleForEarlyReview: Boolean,
    val verdict: ReviewVerdict,
    val explanations: List<String>,
    val requiresExtraConfirmation: Boolean
)

object ReviewEngine {
    fun fraction(done: Number, target: Number): Double =
        if (target.toDouble() <= 0.0) 0.0 else (done.toDouble() / target.toDouble()).coerceAtLeast(0.0)

    /** Não é porcentagem da circunferência atual: é fração da variação entre início e meta. */
    fun goalProgress(site: String, initial: Double, target: Double, current: Double): BodyGoalProgress? {
        if (!initial.isFinite() || !target.isFinite() || !current.isFinite() ||
            initial <= 0 || target <= 0 || current <= 0 || target == initial) return null
        return BodyGoalProgress(site, initial, target, current,
            ((current - initial) / (target - initial)).coerceIn(0.0, 1.0))
    }

    fun analyze(input: ReviewInput): ReviewReport {
        val p = input.parameters
        val days = ((input.nowMillis - input.activeSinceMillis).coerceAtLeast(0L) / 86_400_000L)
        val time = fraction(days, p.windowDays)
        val sessions = fraction(input.completedEffectiveSessions, p.targetSessions)
        val exposures = input.relevantGroups.sorted().associateWith { group ->
            fraction(input.groupExposures[group] ?: 0, p.targetExposuresPerGroup)
        }
        val early = time >= p.earlyReviewFraction || sessions >= p.earlyReviewFraction ||
            exposures.values.any { it >= p.earlyReviewFraction }
        val pain = input.recovery == RecoveryFlag.PERSISTENT_PAIN || input.reason == "DOR_PERSISTENTE"
        val unavailable = input.reason == "EQUIPAMENTO"
        val plateauRequested = input.reason == "ESTAGNACAO"
        val sufficient = input.completedEffectiveSessions >= 4 && (days >= 21 || sessions >= 1.0)
        val verdict = when {
            pain || unavailable -> ReviewVerdict.CAUTION
            early && sufficient && plateauRequested && input.performance == PerformanceTrend.STABLE &&
                input.recovery != RecoveryFlag.TIRED -> ReviewVerdict.VARIATION_POSSIBLE
            early -> ReviewVerdict.REVIEW
            else -> ReviewVerdict.MAINTAIN
        }
        val explanations = buildList {
            add("${days} de ${p.windowDays} dias (${pct(time)}) desde o registro da ficha.")
            add("${input.completedEffectiveSessions} de ${p.targetSessions} sessões efetivas (${pct(sessions)}).")
            if (exposures.isEmpty()) add("Sem grupos prioritários com exposições registradas para comparar.")
            else exposures.forEach { (group,f) ->
                add("$group: ${input.groupExposures[group] ?: 0} de ${p.targetExposuresPerGroup} exposições (${pct(f)}).")
            }
            when(input.performance) {
                PerformanceTrend.IMPROVING -> add("Há indícios de progressão nas séries comparáveis; trocar exercícios não é obrigatório.")
                PerformanceTrend.STABLE -> add("As séries disponíveis não mostram progressão clara; isso não prova um platô fisiológico.")
                PerformanceTrend.INSUFFICIENT -> add("Ainda há poucas séries comparáveis para avaliar progressão.")
            }
            when(input.recovery) {
                RecoveryFlag.TIRED -> add("Você relatou cansaço: vale avaliar carga e recuperação antes de variar exercícios.")
                RecoveryFlag.PERSISTENT_PAIN -> add("Dor persistente relatada: não usar uma troca automática como tratamento; procure avaliação profissional.")
                RecoveryFlag.WELL -> add("Último registro de recuperação: bem.")
                RecoveryFlag.UNKNOWN -> add("Sem registro recente de recuperação; o aplicativo não presume que esteja bem ou mal.")
            }
            if (input.goals.isEmpty()) add("Metas corporais comparáveis: não disponíveis (opcionais).")
            else input.goals.forEach { add("Meta de ${it.site}: ${pct(it.fraction)} da variação planejada; isoladamente não exige nova ficha.") }
            when(verdict) {
                ReviewVerdict.MAINTAIN -> add("Ainda não há indicadores suficientes para recomendar uma troca; você pode pedir variedade por preferência.")
                ReviewVerdict.REVIEW -> add("A revisão já pode ser considerada, mas os dados não justificam trocar a ficha automaticamente.")
                ReviewVerdict.VARIATION_POSSIBLE -> add("Pode valer testar uma variação pontual, após conferir uma proposta equivalente.")
                ReviewVerdict.CAUTION -> add("Priorize segurança: a adaptação deve ser avaliada e não será gerada automaticamente nesta condição.")
            }
        }
        return ReviewReport(days,time,sessions,exposures,early,verdict,explanations,
            verdict != ReviewVerdict.VARIATION_POSSIBLE)
    }
    fun pct(value: Double): String = "${(value * 100.0).roundToInt()}%"
}
