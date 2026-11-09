package br.com.meutreino.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.ExerciseVideoAlternativeCatalog
import br.com.meutreino.domain.ExerciseVideoCatalog
import br.com.meutreino.domain.ExerciseVideoDiscoveryCatalog
import java.net.URLEncoder

/** v0.5.0: uma única ação para vídeo principal: abrir no YouTube.
 * O player WebView experimental foi retirado por falhar na instalação testada.
 */
@Composable
fun ExerciseVideoControl(exerciseId: String) {
    val context = LocalContext.current
    val video = remember(exerciseId) {
        ExerciseVideoAssetRepository.ensureLoaded(context.applicationContext)
        ExerciseVideoCatalog.forExercise(exerciseId)
    }
    if (video != null) {
        OutlineButton("Abrir vídeo no Youtube") {
            openYoutube(context, video.watchUrl)
        }
        if (video.clipped) {
            val start = formatVideoTime(video.startSeconds)
            val end = video.endSeconds?.let(::formatVideoTime)
            Subtext("Trecho recomendado: $start${end?.let { " até $it" } ?: " em diante"}. " +
                "O YouTube pode continuar reproduzindo depois do tempo final selecionado.")
        }
        AdditionalVideoOptions(exerciseId)
    } else {
        val name = ExerciseCatalog.byId(exerciseId)?.name ?: exerciseId
        val query = ExerciseVideoDiscoveryCatalog.queryFor(exerciseId)
            ?: "$name exercício demonstração execução correta"
        Subtext("Ainda não há vídeo aprovado para este exercício. A busca abaixo mostra opções externas não verificadas.")
        OutlineButton("Abrir vídeo no Youtube") {
            openYoutube(context, "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
        }
    }
}

/** As opções alternativas continuam identificadas como não revisadas. */
@Composable
private fun AdditionalVideoOptions(exerciseId: String) {
    val context = LocalContext.current
    val alternatives = remember(exerciseId) {
        ExerciseVideoAssetRepository.ensureLoaded(context.applicationContext)
        ExerciseVideoAlternativeCatalog.forExercise(exerciseId)
    }
    if (alternatives.isEmpty()) return
    var expanded by remember(exerciseId) { mutableStateOf(false) }
    OutlineButton("Quer mais opções de vídeos? ${if (expanded) "▲" else "▼"}") {
        expanded = !expanded
    }
    if (expanded) {
        Subtext("Alternativas da pesquisa, ainda NÃO revisadas visualmente. Confira o movimento antes de utilizá-las.")
        alternatives.forEachIndexed { index, alt ->
            OutlineButton("${index + 1}. ${alt.title} · ${alt.author}") {
                openYoutube(context, alt.watchUrl)
            }
        }
    }
}

private fun openYoutube(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo disponível para abrir o YouTube.", Toast.LENGTH_LONG).show()
    }
}

private fun formatVideoTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
