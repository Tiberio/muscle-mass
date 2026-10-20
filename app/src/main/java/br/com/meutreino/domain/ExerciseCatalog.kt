package br.com.meutreino.domain

/** Catálogo inicial com instruções breves. Vídeos ausentes NÃO são inventados. */
object ExerciseCatalog {
    private val legacy: List<Exercise> = listOf(
        Exercise("pushup", "Flexão de braços", "push", "Peito", setOf("Braços", "Ombros"), advice="Mantenha o corpo alinhado, sem forçar a amplitude."),
        Exercise("inclinepush", "Flexão inclinada", "push", "Peito", setOf("Braços", "Ombros"), advice="Apoie as mãos numa superfície firme e estável."),
        Exercise("dbbench", "Supino com halteres", "push", "Peito", setOf("Braços", "Ombros"), setOf("Halteres", "Banco"), advice="Controle a descida e mantenha ombros e pés estáveis.", youtubeVideoId="XhmFJfVaHjg"),
        Exercise("machinepress", "Supino na máquina", "push", "Peito", setOf("Braços", "Ombros"), setOf("Máquinas"), advice="Ajuste o assento e mantenha as escápulas apoiadas."),
        Exercise("dbrow", "Remada unilateral", "pull", "Costas", setOf("Braços"), setOf("Halteres"), advice="Mantenha o tronco estável, evitando girá-lo."),
        Exercise("bandrow", "Remada com elástico", "pull", "Costas", setOf("Braços"), setOf("Elásticos"), advice="Fixe o elástico num ponto seguro e puxe com controle."),
        Exercise("cable", "Remada baixa", "pull", "Costas", setOf("Braços"), setOf("Máquinas"), advice="Evite movimentar o tronco para impulsionar a carga."),
        Exercise("pullup", "Barra fixa assistida", "pull", "Costas", setOf("Braços"), setOf("Barra fixa"), 1, "Utilize assistência adequada para manter o controle."),
        Exercise("squat", "Agachamento livre", "squat", "Pernas", setOf("Glúteos"), advice="Controle a descida e mantenha os pés firmes."),
        Exercise("goblet", "Agachamento goblet", "squat", "Pernas", setOf("Glúteos"), setOf("Halteres"), advice="Segure a carga junto ao tórax e agache com controle."),
        Exercise("legpress", "Leg press", "squat", "Pernas", setOf("Glúteos"), setOf("Máquinas"), advice="Não perca o apoio do quadril no banco."),
        Exercise("lunge", "Avanço alternado", "lunge", "Pernas", setOf("Glúteos"), advice="Faça passos controlados mantendo boa estabilidade."),
        Exercise("bridge", "Ponte de glúteos", "hinge", "Glúteos", setOf("Pernas"), advice="Eleve o quadril sem hiperestender a coluna."),
        Exercise("rdeadlift", "Levantamento romeno com halteres", "hinge", "Pernas", setOf("Glúteos"), setOf("Halteres"), advice="Flexione o quadril com a coluna neutra e carga próxima ao corpo."),
        Exercise("dbshoulder", "Desenvolvimento com halteres", "verticalPush", "Ombros", setOf("Braços"), setOf("Halteres"), advice="Evite compensar arqueando excessivamente as costas."),
        Exercise("bandshoulder", "Desenvolvimento com elástico", "verticalPush", "Ombros", setOf("Braços"), setOf("Elásticos"), advice="Use resistência que permita movimento controlado."),
        Exercise("lateral", "Elevação lateral", "isolation", "Ombros", setOf("Braços"), setOf("Halteres"), advice="Eleve os braços com controle, sem impulsionar o tronco."),
        Exercise("curl", "Rosca alternada", "isolation", "Braços", emptySet(), setOf("Halteres"), advice="Evite balançar o corpo para levantar o peso."),
        Exercise("bandcurl", "Rosca com elástico", "isolation", "Braços", emptySet(), setOf("Elásticos"), advice="Mantenha os cotovelos próximos ao corpo."),
        Exercise("triceps", "Tríceps com elástico", "isolation", "Braços", emptySet(), setOf("Elásticos"), advice="Evite movimentar o ombro durante a extensão."),
        Exercise("plank", "Prancha", "core", "Abdômen", advice="Mantenha o corpo alinhado; termine antes de perder o controle."),
        Exercise("deadbug", "Dead bug", "core", "Abdômen", advice="Movimente braços e pernas sem perder o controle lombar."),
        Exercise("bird", "Bird dog", "core", "Abdômen", setOf("Costas"), advice="Estenda membros opostos mantendo o tronco estável.")
    )
    private val progressionChains = listOf(
        listOf("wallpush", "inclinepush", "kneepush", "pushup", "tempopush", "declinepush", "archerpush"),
        listOf("pikepush", "elevatedpike", "wallhandstandpush"),
        listOf("scapularpull", "negativepull", "pullup", "strictpull", "archerpull"),
        listOf("regressedsquat", "squat", "pausesquat", "pistolsupport", "pistolsquat"),
        listOf("splitbw", "lunge", "bulgarianbw", "dbbulgarian"),
        listOf("bridge", "glutebridgepause", "glutebridgeone"),
        listOf("plank", "sideplank", "sideplankleg"),
        listOf("deadbug", "deadbugextended", "hollowhold", "hollowrock"),
        listOf("calfstand", "calfone", "calfstep"),
        listOf("bandcurl", "curl", "dbhammer", "dbinclinecurl"),
        listOf("bandrow", "dbrow", "dbchestrow", "bbrow")
    )
    private val mistakes = mapOf(
        "push" to "Evite perder o alinhamento do tronco e acelerar sem controle.",
        "pull" to "Evite elevar os ombros ou impulsionar a carga com o tronco.",
        "squat" to "Evite deixar joelhos colapsarem para dentro ou perder o apoio dos pés.",
        "lunge" to "Evite passos instáveis e amplitudes que provoquem dor.",
        "hinge" to "Evite flexionar a lombar para compensar a falta de mobilidade do quadril.",
        "verticalPush" to "Evite arquear excessivamente a lombar durante a elevação.",
        "isolation" to "Evite balanço corporal e cargas que obriguem a compensar o movimento.",
        "calf" to "Evite realizar movimentos rápidos sem controlar o retorno.",
        "carry" to "Evite inclinar o corpo para compensar o peso.",
        "core" to "Evite prender a respiração ou perder o controle da região lombar."
    )
    val all: List<Exercise> by lazy {
        val chainPosition = progressionChains.flatMap { chain -> chain.mapIndexed { i, id -> id to Triple(chain.getOrNull(i-1), chain.getOrNull(i+1), chain.size) } }.toMap()
        (legacy + ExerciseLibraryData.extra).map { ex ->
            val chain = chainPosition[ex.id]
            ex.copy(
                modality = if (ex.required.isEmpty() || ex.required.all { it in setOf("Barra fixa","Paralelas","Suspensão/TRX","Parede","Step","Banco") }) "Calistenia" else ex.modality,
                commonMistakes = mistakes[ex.pattern] ?: "Mantenha amplitude confortável e execução controlada.",
                regressionId = chain?.first,
                progressionId = chain?.second,
                safetyNote = if (ex.minimumLevel >= 2) "Exercício avançado: progrida gradualmente e evite se houver dor ou falta de estabilidade." else "Interrompa se sentir dor ou instabilidade."
            )
        }
    }
    private val byIdIndex: Map<String, Exercise> by lazy { all.associateBy { it.id } }
    fun byId(id: String): Exercise? = byIdIndex[id]
    fun armAction(ex: Exercise): String {
        if (ex.primaryMuscle != "Braços") return ""
        val name = ex.name.lowercase()
        return when {
            "rosca" in name || "bíceps" in name -> "bíceps"
            "tríceps" in name || "coice" in name || "mergulho" in name -> "tríceps"
            else -> ""
        }
    }
    fun equivalents(exercise: Exercise, environment: String, equipment: List<String>, experience: String): List<Exercise> {
        val arm = armAction(exercise)
        return options(environment, equipment, experience).filter { candidate ->
            candidate.pattern == exercise.pattern && candidate.primaryMuscle == exercise.primaryMuscle &&
                (arm.isBlank() || armAction(candidate) == arm)
        }
    }
    fun options(environment: String, equipment: List<String>, experience: String = "Iniciante"): List<Exercise> {
        val level = when(experience) { "Avançado" -> 2; "Intermediário" -> 1; else -> 0 }
        val allowed = when(environment) {
            "Academia" -> setOf("Halteres","Banco","Máquinas","Elásticos","Barra fixa", "Barra e anilhas", "Kettlebell", "Banco Scott", "Paralelas", "Suspensão/TRX", "Parede", "Step")
            "Casa equipada" -> equipment.toSet() + setOf("Parede")
            else -> setOf("Parede")
        }
        return all.filter { it.minimumLevel <= level && allowed.containsAll(it.required) }
    }
}
