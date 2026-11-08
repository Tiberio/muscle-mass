package br.com.meutreino.domain

/**
 * Dados e índice de demonstrações. Desde a v0.4.2, os registros vêm exclusivamente de
 * assets/exercise_videos.json, carregados uma vez pelo app Android.
 * Nenhuma URL ou associação exercício/vídeo fica compilada neste Kotlin.
 */
data class ExerciseVideo(
    val exerciseId: String,
    val youtubeId: String,
    val author: String,
    val title: String,
    val startSeconds: Int = 0,
    val endSeconds: Int? = null,
    val verification: String = "pending_visual_review"
) {
    init {
        require(exerciseId.isNotBlank())
        require(youtubeId.matches(Regex("[a-zA-Z0-9_-]{11}")))
        require(author.isNotBlank() && title.isNotBlank())
        require(startSeconds >= 0)
        require(endSeconds == null || endSeconds > startSeconds)
    }
    val embedUrl: String get() = buildString {
        append("https://www.youtube.com/embed/").append(youtubeId)
        append("?playsinline=1&controls=1&rel=0")
        if (startSeconds > 0) append("&start=").append(startSeconds)
        endSeconds?.let { append("&end=").append(it) }
    }
    val watchUrl: String get() = buildString {
        append("https://www.youtube.com/watch?v=").append(youtubeId)
        if (startSeconds > 0) append("&t=").append(startSeconds).append("s")
    }
    val clipped: Boolean get() = startSeconds > 0 || endSeconds != null
}

object ExerciseVideoCatalog {
    @Volatile private var byExercise: Map<String, ExerciseVideo> = emptyMap()
    val all: List<ExerciseVideo> get() = byExercise.values.toList()
    fun forExercise(id: String): ExerciseVideo? = byExercise[id]
    /** Chamado apenas após validar cada entrada do JSON. */
    @Synchronized fun replaceWith(videos: List<ExerciseVideo>) {
        require(videos.map { it.exerciseId }.distinct().size == videos.size) {
            "IDs de exercícios duplicados no catálogo de vídeo"
        }
        byExercise = videos.associateBy { it.exerciseId }
    }
}


/** Buscas específicas para variantes ainda sem vídeo pré-selecionado e validado. */
object ExerciseVideoDiscoveryCatalog {
    @Volatile private var searchByExercise: Map<String, String> = emptyMap()
    fun queryFor(exerciseId: String): String? = searchByExercise[exerciseId]
    @Synchronized fun replaceWith(searches: Map<String, String>) {
        require(searches.keys.none { it.isBlank() })
        searchByExercise = searches.toMap()
    }
}


/** Vídeo individual identificado por metadados, mas ainda sem revisão de movimento/trecho.
 * NÃO é convertido automaticamente em ExerciseVideo nem incorporado ao player.
 */
data class ExerciseVideoCandidate(
    val exerciseId: String,
    val youtubeId: String,
    val matchedTitle: String,
    val source: String,
    val durationSecondsFromSource: Int? = null
) {
    init {
        require(exerciseId.isNotBlank())
        require(youtubeId.matches(Regex("[a-zA-Z0-9_-]{11}")))
        require(matchedTitle.isNotBlank())
        require(source.isNotBlank())
        require(durationSecondsFromSource == null || durationSecondsFromSource > 0)
    }
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$youtubeId"
}

object ExerciseVideoCandidateCatalog {
    @Volatile private var byExercise: Map<String, ExerciseVideoCandidate> = emptyMap()
    val all: List<ExerciseVideoCandidate> get() = byExercise.values.toList()
    fun forExercise(id: String): ExerciseVideoCandidate? = byExercise[id]
    @Synchronized fun replaceWith(candidates: List<ExerciseVideoCandidate>) {
        require(candidates.map { it.exerciseId }.distinct().size == candidates.size) {
            "IDs duplicados nos candidatos de vídeo"
        }
        byExercise = candidates.associateBy { it.exerciseId }
    }
}


/** Opção adicional catalogada pela pesquisa, mas NÃO aprovada visualmente.
 * Ela nunca entra no player incorporado e é aberta exclusivamente no YouTube.
 */
data class ExerciseVideoAlternative(
    val exerciseId: String,
    val youtubeId: String,
    val title: String,
    val author: String,
    val durationSeconds: Int? = null
) {
    init {
        require(exerciseId.isNotBlank())
        require(youtubeId.matches(Regex("[a-zA-Z0-9_-]{11}")))
        require(title.isNotBlank() && author.isNotBlank())
        require(durationSeconds == null || durationSeconds > 0)
    }
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$youtubeId"
}

object ExerciseVideoAlternativeCatalog {
    @Volatile private var byExercise: Map<String, List<ExerciseVideoAlternative>> = emptyMap()
    fun forExercise(id: String): List<ExerciseVideoAlternative> = byExercise[id].orEmpty()
    val count: Int get() = byExercise.values.sumOf { it.size }
    @Synchronized fun replaceWith(alternatives: List<ExerciseVideoAlternative>) {
        require(alternatives.map { it.exerciseId to it.youtubeId }.distinct().size == alternatives.size) {
            "Vídeos alternativos duplicados para o mesmo exercício"
        }
        byExercise = alternatives.groupBy { it.exerciseId }
    }
}
