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
        // Os slots descrevem movimento + músculo principal; não basta alternar a ordem.
        // Em >=4 dias, as ênfases alternam superior/inferior; cardio é planejado à parte.
        data class Slot(val pattern: String, val target: String? = null)
        val upperA = listOf(Slot("push","Peito"),Slot("pull","Costas"),Slot("verticalPush","Ombros"),Slot("isolation","Braços"),Slot("core","Abdômen"))
        val upperB = listOf(Slot("pull","Costas"),Slot("push","Peito"),Slot("isolation","Braços"),Slot("verticalPush","Ombros"),Slot("core","Abdômen"))
        val upperC = listOf(Slot("push","Peito"),Slot("pull","Costas"),Slot("isolation","Ombros"),Slot("isolation","Braços"),Slot("core","Abdômen"))
        val lowerA = listOf(Slot("squat","Pernas"),Slot("hinge"),Slot("lunge"),Slot("calf","Pernas"),Slot("core","Abdômen"))
        val lowerB = listOf(Slot("hinge"),Slot("lunge"),Slot("squat","Pernas"),Slot("core","Abdômen"),Slot("calf","Pernas"))
        val fullA = listOf(Slot("push"),Slot("pull"),Slot("squat"),Slot("core"),Slot("isolation"))
        val fullB = listOf(Slot("hinge"),Slot("lunge"),Slot("pull"),Slot("verticalPush"),Slot("core"))
        val fullC = listOf(Slot("push"),Slot("pull"),Slot("squat"),Slot("hinge"),Slot("isolation"))
        val templates = when(count) {
            1 -> listOf(listOf(Slot("push"),Slot("pull"),Slot("squat"),Slot("hinge"),Slot("core")))
            2 -> listOf(fullA,fullB)
            3 -> listOf(fullA,fullB,fullC)
            else -> listOf(upperA,lowerA,upperB,lowerB,upperC,lowerA,upperB)
        }
        val usedExerciseIds = mutableMapOf<String, Int>()
        val previousByPattern = mutableMapOf<String, String>()
        val warnings = mutableSetOf<String>()
        if (pool.none { it.pattern == "pull" && it.primaryMuscle == "Costas" })
            warnings += "Neste ambiente não há equipamento de puxada cadastrado; movimentos de costas não têm substituto equivalente. Considere um elástico, barra fixa ou academia."
        val sets = when(p.experience) { "Iniciante" -> 2; else -> 3 }
        val sessions = preferredDays.mapIndexed { index, day ->
            val slots = templates[index % templates.size].take(exerciseCount)
            val selected = mutableListOf<Exercise>()
            val upperDay = count>=4 && index%2==0
            val lowerDay = count>=4 && !upperDay
            slots.forEach { slot ->
                val candidates = pool.filter { ex ->
                    ex.pattern == slot.pattern && ex !in selected &&
                        (slot.target == null || ex.primaryMuscle == slot.target)
                }
                // Evitar repetição indiscriminada e preferir progressões acessíveis.
                val preferred = candidates.sortedWith(
                    compareBy<Exercise> { usedExerciseIds[it.id] ?: 0 }
                        .thenBy { if (previousByPattern[it.pattern] == it.id) 1 else 0 }
                        .thenBy { if (slot.pattern == "isolation" && slot.target == "Braços" && ExerciseCatalog.armAction(it) != if(index%3==1) "tríceps" else "bíceps") 1 else 0 }
                        .thenBy { if (it.primaryMuscle in p.priorityMuscles) 0 else 1 }
                        .thenBy { it.minimumLevel }
                        .thenBy { it.required.size }
                        .thenBy { it.id }
                )
                preferred.firstOrNull()?.let(selected::add)
            }
            // Completar somente com exercícios do segmento planejado: sem preencher
            // um treino de membros superiores com agachamentos quando falta uma puxada.
            val fillPatterns = when {
                upperDay -> setOf("push","pull","verticalPush","isolation","core")
                lowerDay -> setOf("squat","hinge","lunge","calf","core")
                else -> setOf("push","pull","verticalPush","isolation","squat","hinge","lunge","calf","core")
            }
            val allowedMuscles = when {
                upperDay -> setOf("Peito","Costas","Ombros","Braços","Abdômen")
                lowerDay -> setOf("Pernas","Glúteos","Abdômen")
                else -> setOf("Peito","Costas","Ombros","Braços","Pernas","Glúteos","Abdômen")
            }
            val extras = pool.filter { it.pattern in fillPatterns && it.primaryMuscle in allowedMuscles && it !in selected }
                .sortedWith(compareBy<Exercise> { usedExerciseIds[it.id] ?: 0 }
                    .thenBy { if (it.primaryMuscle in p.priorityMuscles) 0 else 1 }
                    .thenBy { if (it.pattern == "core" && selected.any { e -> e.pattern == "core" }) 1 else 0 }
                    .thenBy { it.minimumLevel }.thenBy { it.id })
            for (exercise in extras) {
                if (selected.size>=exerciseCount) break
                // Não repetir o mesmo padrão e músculo principal quando há outras opções.
                val sameGroup = selected.count{ it.pattern==exercise.pattern && it.primaryMuscle==exercise.primaryMuscle }
                if (sameGroup >= 2) continue
                selected += exercise
            }
            val unique = selected.take(exerciseCount)
            unique.forEach { usedExerciseIds[it.id] = (usedExerciseIds[it.id] ?: 0) + 1;previousByPattern[it.pattern] = it.id }
            PlannedSession(
                id = "T${index + 1}", title = if(count>=4) when(index%7) {
                    0 -> "Superiores A"; 1 -> "Inferiores A"; 2 -> "Superiores B";
                    3 -> "Inferiores B"; 4 -> "Superiores C"; 5 -> "Inferiores C";
                    else -> "Superiores D"
                } else when(index) { 0 -> "Força e equilíbrio A"; 1 -> "Força e equilíbrio B"; else -> "Corpo inteiro C" },
                day = p.assignments["T${index + 1}"] ?: day,
                minutes = minutes,
                exercises = unique.map { PlannedExercise(it, sets, if(p.objective=="Força") 6 else 8, if(p.objective=="Força") 10 else 12) },
                explanation = "Ênfase: ${if(upperDay)"superiores" else if(lowerDay)"inferiores" else "corpo inteiro"}; variação dos exercícios sem perder o padrão de movimento e a progressão.",
                warning = if (count>=4 && upperDay && unique.none { it.pattern=="pull" && it.primaryMuscle=="Costas" }) "Sem puxada equivalente neste ambiente." else null
            )
        }
        if (sessions.any { it.exercises.size < exerciseCount }) warnings += "Algumas sessões têm menos exercícios do que o solicitado porque faltam alternativas compatíveis; não adicionamos substituições de músculos diferentes artificialmente."
        if (p.limitations.isNotBlank()) warnings += "Você informou limitações. Revise os exercícios individualmente antes de iniciar; a triagem de dor e restrições ainda não é automática."
        if(p.cardioWeeklyDays==7 && p.cardioActivities.isNotEmpty())warnings += "Cardio diário mantido: varie o esforço e use atividades leves nos dias de recuperação da musculação. Não é necessário fazer cardio vigoroso sete dias."
        if (count >= 5) warnings += "Treino frequente: alternamos sessões predominantemente superiores e inferiores, mas a recuperação real depende de volume, esforço, sono e limitações."
        if (count == 3 && preferredDays.zipWithNext().any { (a,b)-> allDays.indexOf(b)-allDays.indexOf(a)==1 }) warnings += "Você programou musculação em dias consecutivos. Revise a recuperação de músculos principais e secundários."
        if (count >= 6) warnings += "Frequência alta: ajuste intensidade, volume e recuperação antes de executar todas as sessões."
        val explanation = "O plano usa ${sessions.size} sessão(ões) de musculação de $minutes minutos cada. " +
            "${if(p.priorityMuscles.isEmpty())"Distribuição geral do corpo." else "Ênfase em ${p.priorityMuscles.joinToString(", ")}, sem excluir pernas e costas quando o equipamento permite."} " +
            "O cardio, quando selecionado, é planejado com duração adicional e independente. Exercícios e duração são estimativas iniciais ajustáveis."
        return TrainingPlan(sessions, explanation, warnings.toList())
    }
}
