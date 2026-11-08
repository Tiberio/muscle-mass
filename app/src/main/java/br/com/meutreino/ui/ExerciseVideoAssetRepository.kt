package br.com.meutreino.ui

import android.content.Context
import android.util.Log
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.ExerciseVideo
import br.com.meutreino.domain.ExerciseVideoCatalog
import br.com.meutreino.domain.ExerciseVideoAlternative
import br.com.meutreino.domain.ExerciseVideoAlternativeCatalog
import br.com.meutreino.domain.ExerciseVideoCandidate
import br.com.meutreino.domain.ExerciseVideoCandidateCatalog
import br.com.meutreino.domain.ExerciseVideoDiscoveryCatalog
import org.json.JSONObject

/** Carrega o JSON do APK; nunca baixa catálogo remoto nem altera o histórico do usuário. */
internal object ExerciseVideoAssetRepository {
    @Volatile private var initialized = false

    @Synchronized fun ensureLoaded(context: Context) {
        if (initialized) return
        try {
            val raw = context.assets.open("exercise_videos.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val catalog = JSONObject(raw)
            require(catalog.getInt("schema_version") == 1) { "Versão de esquema não suportada" }
            val rows = catalog.getJSONArray("videos")
            val parsed = ArrayList<ExerciseVideo>(rows.length())
            val seen = HashSet<String>()
            for (i in 0 until rows.length()) {
                try {
                    val row = rows.getJSONObject(i)
                    val id = row.getString("exercise_id")
                    require(ExerciseCatalog.byId(id) != null) { "Exercício inexistente: $id" }
                    require(seen.add(id)) { "Vídeo duplicado para $id" }
                    val end = if (row.isNull("end_seconds") || !row.has("end_seconds")) null
                              else row.getInt("end_seconds")
                    parsed.add(ExerciseVideo(
                        exerciseId = id,
                        youtubeId = row.getString("youtube_id"),
                        author = row.getString("author"),
                        title = row.getString("title"),
                        startSeconds = row.optInt("start_seconds", 0),
                        endSeconds = end,
                        verification = row.optString("verification", "pending_visual_review")
                    ))
                } catch (err: Exception) {
                    // Um registro ruim não bloqueia os vídeos válidos, nem a tela de treino.
                    Log.w("MeuTreinoVideos", "Ignorando entrada de vídeo #$i", err)
                }
            }
            ExerciseVideoCatalog.replaceWith(parsed)
            // Links externos de opções ainda sem revisão visual, nunca exibidas no player.
            val alternativeRows = catalog.optJSONArray("alternatives")
            val alternatives = ArrayList<ExerciseVideoAlternative>()
            val alternativeKeys = HashSet<String>()
            if (alternativeRows != null) {
                for (i in 0 until alternativeRows.length()) {
                    try {
                        val row = alternativeRows.getJSONObject(i)
                        val id = row.getString("exercise_id")
                        require(id in seen) { "Alternativa sem vídeo principal aprovado: $id" }
                        require(row.getString("verification") == "not_visually_reviewed")
                        require(row.getString("playback_status") == "external_only")
                        val youtubeId = row.getString("youtube_id")
                        require(alternativeKeys.add("$id:$youtubeId"))
                        require(parsed.none { it.exerciseId == id && it.youtubeId == youtubeId })
                        val duration = if (row.isNull("duration_seconds") || !row.has("duration_seconds"))
                            null else row.getInt("duration_seconds")
                        val alternative = ExerciseVideoAlternative(
                            exerciseId = id,
                            youtubeId = youtubeId,
                            title = row.getString("title"),
                            author = row.getString("author"),
                            durationSeconds = duration
                        )
                        require(row.getString("source_url") == alternative.watchUrl)
                        alternatives.add(alternative)
                    } catch (err: Exception) {
                        Log.w("MeuTreinoVideos", "Ignorando alternativa inválida #$i", err)
                    }
                }
            }
            ExerciseVideoAlternativeCatalog.replaceWith(alternatives)
            // Candidatos têm URL específica, mas não foram conferidos visualmente.
            // Por isso são oferecidos apenas como link externo, não como embed.
            val candidateRows = catalog.optJSONArray("candidates")
            val candidates = ArrayList<ExerciseVideoCandidate>()
            val candidateIds = HashSet<String>()
            if (candidateRows != null) {
                for (i in 0 until candidateRows.length()) {
                    try {
                        val entry = candidateRows.getJSONObject(i)
                        val id = entry.getString("exercise_id")
                        require(ExerciseCatalog.byId(id) != null && id !in seen)
                        require(candidateIds.add(id))
                        require(entry.getString("verification") == "metadata_only")
                        require(entry.getString("playback_status") == "external_review_only")
                        val duration = if (entry.isNull("duration_seconds_from_source") ||
                            !entry.has("duration_seconds_from_source")) null
                            else entry.getInt("duration_seconds_from_source")
                        val candidate = ExerciseVideoCandidate(
                            exerciseId = id,
                            youtubeId = entry.getString("youtube_id"),
                            matchedTitle = entry.getString("matched_title"),
                            source = entry.getString("source"),
                            durationSecondsFromSource = duration
                        )
                        require(entry.getString("source_url") == candidate.watchUrl)
                        candidates.add(candidate)
                    } catch (err: Exception) {
                        Log.w("MeuTreinoVideos", "Ignorando candidato inválido #$i", err)
                    }
                }
            }
            ExerciseVideoCandidateCatalog.replaceWith(candidates)
            // Novos registros no mesmo JSON: procura direcionada somente quando
            // ainda não existe uma demonstração individual selecionada.
            val discovered = HashMap<String, String>()
            val searches = catalog.optJSONArray("discovery")
            if (searches != null) {
                for (i in 0 until searches.length()) {
                    try {
                        val entry = searches.getJSONObject(i)
                        val id = entry.getString("exercise_id")
                        val query = entry.getString("search_query").trim()
                        require(ExerciseCatalog.byId(id) != null && query.isNotBlank())
                        require(id !in discovered && id !in seen && id !in candidateIds)
                        discovered[id] = query
                    } catch (err: Exception) {
                        Log.w("MeuTreinoVideos", "Ignorando busca inválida #$i", err)
                    }
                }
            }
            ExerciseVideoDiscoveryCatalog.replaceWith(discovered)
            Log.i("MeuTreinoVideos", "Catálogo: ${parsed.size} vídeos aprovados/prévios, ${alternatives.size} opções externas não revisadas, ${candidates.size} candidatos")
        } catch (err: Exception) {
            Log.e("MeuTreinoVideos", "Falha ao carregar catálogo local de vídeos", err)
            ExerciseVideoCatalog.replaceWith(emptyList())
            ExerciseVideoAlternativeCatalog.replaceWith(emptyList())
            ExerciseVideoCandidateCatalog.replaceWith(emptyList())
            ExerciseVideoDiscoveryCatalog.replaceWith(emptyMap())
        }
        initialized = true
    }
}
