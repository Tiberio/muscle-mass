package br.com.meutreino.ui

import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.ExerciseVideoDiscoveryCatalog
import br.com.meutreino.domain.ExerciseVideoCandidateCatalog
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.meutreino.domain.ExerciseVideoCatalog
import br.com.meutreino.domain.ExerciseVideoAlternativeCatalog

/** Player oficial incorporado; só começa a carregar após o usuário solicitar a demonstração. */
@Composable
fun ExerciseVideoControl(exerciseId: String) {
    val context = LocalContext.current
    // O catálogo é carregado uma única vez e vinculado pelos IDs estáveis dos exercícios.
    val video = remember(exerciseId) {
        ExerciseVideoAssetRepository.ensureLoaded(context.applicationContext)
        ExerciseVideoCatalog.forExercise(exerciseId)
    }
    var open by remember(exerciseId) { mutableStateOf(false) }
    if (video == null) {
        val name = ExerciseCatalog.byId(exerciseId)?.name ?: exerciseId
        val candidate = ExerciseVideoCandidateCatalog.forExercise(exerciseId)
        val query = ExerciseVideoDiscoveryCatalog.queryFor(exerciseId)
            ?: "$name exercício demonstração execução correta"
        if (candidate != null) {
            Subtext("Vídeo candidato: ${candidate.matchedTitle}.")
            Subtext("Correspondência por metadados: ainda sem revisão visual do movimento e do trecho.")
            candidate.durationSecondsFromSource?.let { seconds ->
                Subtext("Duração informada pela fonte: ${seconds}s (não conferida no player).")
            }
            OutlineButton("▶ Abrir vídeo candidato no YouTube", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(candidate.watchUrl)))
            })
        } else {
            Subtext("Ainda não há vídeo individual identificado para esta variante.")
        }
        OutlineButton("🔎 Buscar outra demonstração no YouTube", onClick = {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        })
        Subtext("A busca apresenta opções externas. Confira o nome, a variação e o equipamento do exercício antes de seguir o vídeo.")
        return
    }
    if (!open) {
        OutlineButton("▶ Ver demonstração", onClick = { open = true })
        AdditionalVideoOptions(exerciseId)
        return
    }
    Subtext("Demonstração: ${video.title} · ${video.author}")
    if (video.clipped) Subtext("Trecho selecionado do vídeo original.")
    if (video.verification != "visually_reviewed")
        Subtext("Vídeo associado pelo título ou descrição; técnica ainda pendente de revisão visual.")
    // Não criamos cópia de material de terceiros; o player é servido pelo YouTube.
    key(video.exerciseId, video.youtubeId, video.startSeconds, video.endSeconds) {
        val currentView = remember { arrayOfNulls<WebView>(1) }
        DisposableEffect(Unit) {
            onDispose {
                currentView[0]?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    destroy()
                }
                currentView[0] = null
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            factory = { appContext ->
                WebView(appContext).also { view ->
                    view.settings.javaScriptEnabled = true
                    view.settings.domStorageEnabled = true
                    view.settings.mediaPlaybackRequiresUserGesture = true
                    view.webChromeClient = WebChromeClient()
                    view.webViewClient = WebViewClient()
                    // Identifica o aplicativo ao YouTube, conforme as regras de embeds em WebView.
                    view.loadUrl(
                        video.embedUrl,
                        mapOf("Referer" to "android-app://${appContext.packageName}")
                    )
                    currentView[0] = view
                }
            }
        )
    }
    OutlineButton("Abrir original no YouTube", onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
    })
    AdditionalVideoOptions(exerciseId)
    OutlineButton("Fechar demonstração", onClick = { open = false })
    Subtext("Se o criador bloquear a incorporação, use 'Abrir original no YouTube'. Requer internet.")
}


/** Os vídeos deste painel são somente alternativas não conferidas; não trocar
 * automaticamente a demonstração principal aprovada pelo usuário.
 */
@Composable
private fun AdditionalVideoOptions(exerciseId: String) {
    val context = LocalContext.current
    val alternatives = remember(exerciseId) {
        ExerciseVideoAssetRepository.ensureLoaded(context.applicationContext)
        ExerciseVideoAlternativeCatalog.forExercise(exerciseId)
    }
    if (alternatives.isEmpty()) return
    var expanded by remember(exerciseId) { mutableStateOf(false) }
    OutlineButton("Quer mais opções de vídeos? ${if (expanded) "▲" else "▼"}", onClick = {
        expanded = !expanded
    })
    if (expanded) {
        Subtext("Alternativas encontradas na pesquisa, ainda NÃO revisadas visualmente. " +
            "Confira o movimento e o equipamento antes de utilizá-las.")
        alternatives.forEachIndexed { index, alternative ->
            OutlineButton("${index + 1}. ${alternative.title} · ${alternative.author}", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alternative.watchUrl)))
            })
        }
    }
}
