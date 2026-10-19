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
        // minutesPerSession representa APENAS a musculação: o cardio tem duração independente.
        // Reservamos 5 minutos da sessão de força para aquecimento, sem descontar cardioMinutes.
        // Estimativa inicial: até cinco exercícios, cerca de nove minutos por exercício.
        val availableMinutes = (minutes - 5).coerceAtLeast(10)
        val exerciseCount = (availableMinutes / 9).coerceIn(2, 5)
        val environment = p.defaultEnvironment.ifBlank { p.environments.firstOrNull() ?: "Casa sem equipamentos" }
        val pool = ExerciseCatalog.options(environment, p.equipments, p.experience)
        // Em >=4 dias, priorizar alternância de membros superiores e inferiores,
        // mantendo 48 h entre sessões intensas predominantes de um mesmo grupo.
        val upperA = listOf("push","pull","verticalPush","isolation","core")
        val lowerA = listOf("squat","hinge","lunge","core","pull")
        val upperB = listOf("pull","verticalPush","push","isolation","core")
        val lowerB = listOf("hinge","lunge","squat","core","pull")
        val upperC = listOf("push","pull","isolation","verticalPush","core")
        val templates = when {
            count == 1 -> listOf(listOf("push","pull","squat","hinge","core"))
            count == 2 -> listOf(listOf("push","pull","squat","core","isolation"),listOf("verticalPush","pull","hinge","lunge","core"))
            count == 3 -> listOf(listOf("push","pull","squat","core","isolation"),listOf("hinge","lunge","verticalPush","pull","core"),listOf("push","pull","squat","verticalPush","isolation"))
            else -> listOf(upperA,lowerA,upperB,lowerB,upperC,lowerA,upperB)
        }
        val usedExerciseIds = mutableMapOf<String, Int>()
        val warnings = mutableSetOf<String>()
        if (pool.none { it.pattern == "pull" }) warnings += "Neste ambiente não há equipamento de puxada cadastrado; movimentos de costas não têm substituto equivalente. Considere um elástico, barra fixa ou academia."
        val sets = when(p.experience) { "Iniciante" -> 2; else -> 3 }
        val sessions = preferredDays.mapIndexed { index, day ->
            val desired = templates[index % templates.size].take(exerciseCount)
            val selected = mutableListOf<Exercise>()
            desired.forEach { pattern ->
                val candidates = pool.filter { it.pattern == pattern && it !in selected }
                val preferred = candidates.sortedWith(
                    compareBy<Exercise> { usedExerciseIds[it.id] ?: 0 }
                        .thenByDescending { it.primaryMuscle in p.priorityMuscles }
                        .thenBy { it.required.size }
                )
                if (preferred.isNotEmpty()) selected += preferred.first()
            }
            // Se não há equipamento para determinada categoria, não falsificar equivalência.
            val replacementPatterns = listOf("core","squat","hinge","lunge","push","verticalPush","isolation","pull")
            replacementPatterns.forEach { pattern ->
                if (selected.size < exerciseCount) pool.firstOrNull { it.pattern == pattern && it !in selected }?.let(selected::add)
            }
            val unique = selected.take(exerciseCount)
            unique.forEach { usedExerciseIds[it.id] = (usedExerciseIds[it.id] ?: 0) + 1 }
            val session = PlannedSession(
                id = "T${index + 1}", title = if(count>=4) when(index%7) {
                    0 -> "Superiores A"; 1 -> "Inferiores A"; 2 -> "Superiores B";
                    3 -> "Inferiores B"; 4 -> "Superiores C"; 5 -> "Inferiores C";
                    else -> "Superiores D"
                } else when(index) { 0 -> "Força e equilíbrio A"; 1 -> "Força e equilíbrio B"; else -> "Corpo inteiro C" },
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
        if(p.cardioWeeklyDays==7 && p.cardioActivities.isNotEmpty())warnings += "Cardio diário mantido: recomenda-se variar o esforço e usar atividades leves nos dias de recuperação da musculação. Não é necessário fazer cardio vigoroso sete dias."
        if (count >= 5) warnings += "Treino frequente: distribuímos sessões predominantemente superiores e inferiores, mas a recuperação real depende de volume, esforço, sono e limitações. Consulte os dias e ajuste a carga."
        if (count == 3 && preferredDays.zipWithNext().any { (a,b)-> allDays.indexOf(b)-allDays.indexOf(a)==1 }) warnings += "Você programou musculação em dias consecutivos. Revise a recuperação de músculos principais e secundários."
        if (count >= 6) warnings += "Frequência alta: ajuste intensidade, volume e recuperação antes de executar todas as sessões."
        val explanation = "O plano usa ${sessions.size} sessão(ões) de musculação de $minutes minutos cada. " +
            "${if(p.priorityMuscles.isEmpty())"Distribuição geral do corpo." else "Ênfase em ${p.priorityMuscles.joinToString(", ")}, sem excluir pernas e costas quando o equipamento permite."} " +
            "O cardio, quando selecionado, é planejado com duração adicional e independente. Exercícios e duração são estimativas iniciais ajustáveis."
        return TrainingPlan(sessions, explanation, warnings.toList())
    }
}
