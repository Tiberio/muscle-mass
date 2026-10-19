package br.com.meutreino.domain

/** Núcleo Kotlin puro, determinístico e testável sem Android. Não prescreve tratamento médico. */
object TrainingPlanner {
    private val allDays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    fun build(p: UserPrefs): TrainingPlan {
        val preferredDays = if (p.scheduleMode == "Dias definidos" && p.weekdays.isNotEmpty()) {
            allDays.filter { it in p.weekdays }
        } else {
            val sample = listOf("Seg", "Qua", "Sex", "Sáb", "Ter", "Qui", "Dom")
            sample.take(p.weeklyCount.coerceIn(1, 7)).sortedBy { allDays.indexOf(it) }
        }
        val count = preferredDays.size.coerceAtLeast(1)
        val minutes = p.minutesPerSession.coerceIn(10, 240)
        // Limite aproximado, incluindo pausas e aquecimento. Evita propor 6 exercícios em 20 min.
        val availableMinutes = (minutes - (if (p.cardioActivities.isNotEmpty() && p.cardioWhen == "Após musculação") p.cardioMinutes else 0) - 5).coerceAtLeast(10)
        val exerciseCount = (availableMinutes / 9).coerceIn(2, 5)
        val environment = p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val pool = ExerciseCatalog.options(environment, p.equipments, p.experience)
        val templates = when(count) {
            1 -> listOf(listOf("push","pull","squat","hinge","core"))
            2 -> listOf(listOf("push","pull","squat","core","isolation"),listOf("verticalPush","pull","hinge","lunge","core"))
            3 -> listOf(listOf("push","pull","squat","isolation","core"),listOf("hinge","lunge","verticalPush","pull","core"),listOf("push","pull","squat","verticalPush","isolation"))
            else -> listOf(listOf("push","pull","isolation","core","verticalPush"),listOf("squat","hinge","lunge","core","pull"),listOf("verticalPush","pull","push","isolation","core"),listOf("squat","hinge","lunge","core","pull"))
        }
        val warnings = mutableSetOf<String>()
        if (pool.none { it.pattern == "pull" }) warnings += "Neste ambiente não há equipamento de puxada cadastrado; movimentos de costas não têm substituto equivalente. Considere um elástico, barra fixa ou academia."
        val sets = when(p.experience) { "Iniciante" -> 2; else -> 3 }
        val sessions = preferredDays.mapIndexed { index, day ->
            val desired = templates[index % templates.size].take(exerciseCount)
            val selected = mutableListOf<Exercise>()
            desired.forEach { pattern ->
                val candidates = pool.filter { it.pattern == pattern && it !in selected }
                val preferred = candidates.sortedWith(compareByDescending<Exercise> { it.primaryMuscle in p.priorityMuscles }.thenBy { it.required.size })
                if (preferred.isNotEmpty()) selected += preferred.first()
            }
            // Se não há equipamento para determinada categoria, não falsificar equivalência.
            val replacementPatterns = listOf("core","squat","hinge","lunge","push","verticalPush","isolation","pull")
            replacementPatterns.forEach { pattern ->
                if (selected.size < exerciseCount) pool.firstOrNull { it.pattern == pattern && it !in selected }?.let(selected::add)
            }
            val unique = selected.take(exerciseCount)
            val session = PlannedSession(
                id = "T${index + 1}", title = when(index % templates.size) { 0 -> "Força e equilíbrio A"; 1 -> "Força e equilíbrio B"; 2 -> "Corpo inteiro com ênfase"; else -> "Força complementar" },
                day = p.assignments["T${index + 1}"] ?: day,
                minutes = minutes,
                exercises = unique.map { PlannedExercise(it, sets, if(p.objective=="Força") 6 else 8, if(p.objective=="Força") 10 else 12) },
                explanation = "Prioridade: ${p.priorityMuscles.joinToString(", ").ifBlank { "saúde global" }}; também distribuímos padrões de empurrar, puxar, pernas e core.",
                warning = if (pool.none { it.pattern == "pull" }) "Sem puxada equivalente neste ambiente." else null
            )
            session
        }
        if (sessions.any { it.exercises.isEmpty() }) warnings += "Poucos exercícios compatíveis com os equipamentos informados."
        if (p.limitations.isNotBlank()) warnings += "Você informou limitações. Revise os exercícios individualmente antes de iniciar; o protótipo ainda não faz triagem clínica."
        if (count >= 6) warnings += "Frequência alta: ajuste intensidade, volume e recuperação antes de executar todas as sessões."
        val explanation = "O plano usa ${sessions.size} sessão(ões) de $minutes minutos. ${if(p.priorityMuscles.isEmpty())"Distribuição geral do corpo." else "Ênfase em ${p.priorityMuscles.joinToString(", ")}, sem excluir pernas e costas quando o equipamento permite."} Exercícios e duração são estimativas iniciais ajustáveis."
        return TrainingPlan(sessions, explanation, warnings.toList())
    }
}
