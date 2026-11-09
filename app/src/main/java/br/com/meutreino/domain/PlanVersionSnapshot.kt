package br.com.meutreino.domain

import java.security.MessageDigest

/** Registro estável da ficha ativa, independente da versão instalada do aplicativo.
 * A versão muda quando o conteúdo real do plano muda; nunca zera as sessões antigas.
 */
object PlanVersionSnapshot {
    fun content(p: UserPrefs, environment: String): String {
        val actual = p.copy(defaultEnvironment = environment)
        val plan = TrainingPlanner.build(actual)
        return buildString {
            append("Ambiente: ").append(environment).append('\n')
            append("Objetivo: ").append(p.objective).append('\n')
            append("Duração de musculação: ").append(p.minutesPerSession).append(" min\n")
            plan.sessions.forEach { session ->
                append(session.id).append(" · ").append(session.day)
                    .append(" · ").append(session.title).append('\n')
                session.exercises.forEach { item ->
                    append("  ").append(item.exercise.id).append(" | ")
                        .append(item.exercise.name).append(" | ")
                        .append(item.sets).append(" séries | ")
                        .append(item.minReps).append('-').append(item.maxReps).append(" reps\n")
                }
            }
        }
    }

    fun fingerprint(content: String): String = MessageDigest.getInstance("SHA-256")
        .digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
