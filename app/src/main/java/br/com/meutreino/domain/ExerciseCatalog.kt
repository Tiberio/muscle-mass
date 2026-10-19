package br.com.meutreino.domain

/** Catálogo inicial com instruções breves. Vídeos ausentes NÃO são inventados. */
object ExerciseCatalog {
    val all: List<Exercise> = listOf(
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
    fun byId(id: String): Exercise? = all.firstOrNull { it.id == id }
    fun options(environment: String, equipment: List<String>, experience: String = "Iniciante"): List<Exercise> {
        val level = when(experience) { "Avançado" -> 2; "Intermediário" -> 1; else -> 0 }
        val allowed = when(environment) {
            "Academia" -> setOf("Halteres","Banco","Máquinas","Elásticos","Barra fixa")
            "Casa equipada" -> equipment.toSet()
            else -> emptySet()
        }
        return all.filter { it.minimumLevel <= level && allowed.containsAll(it.required) }
    }
}
