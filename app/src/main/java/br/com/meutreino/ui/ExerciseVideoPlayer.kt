package br.com.meutreino.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URLEncoder
import br.com.meutreino.domain.ExerciseCatalog
import br.com.meutreino.domain.ExerciseVideoDiscoveryCatalog
import br.com.meutreino.domain.ExerciseVideoCandidateCatalog
import android.util.Log
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
    var playbackError by remember(exerciseId) { mutableStateOf<String?>(null) }
    var reloadCount by remember(exerciseId) { mutableIntStateOf(0) }
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
        // Caminho principal: aba de navegador vinculada ao app (Custom Tabs),
        // recomendado pelo YouTube. Evita o WebView que falhou nos testes reais.
        OutlineButton("▶ Ver demonstração", onClick = {
            openEmbeddedInCustomTab(context, video.embedUrl, video.watchUrl)
        })
        OutlineButton("Abrir original no YouTube", onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
        })
        if (video.clipped) {
            val beginning = formatVideoTime(video.startSeconds)
            val ending = video.endSeconds?.let(::formatVideoTime)
            Subtext("Trecho recomendado: $beginning${ending?.let { " até $it" } ?: " em diante"}. " +
                "A abertura pelo YouTube pode não encerrar automaticamente no tempo final.")
        }
        Subtext("A demonstração abre em uma aba do navegador, permitindo voltar ao treino. " +
            "O player incorporado na tela continua disponível apenas para diagnóstico.")
        AdditionalVideoOptions(exerciseId)
        OutlineButton("Testar player incorporado (experimental)", onClick = {
            playbackError = null
            open = true
        })
        return
    }
    Subtext("Demonstração: ${video.title} · ${video.author}")
    if (video.clipped) Subtext("Trecho selecionado do vídeo original.")
    if (video.verification != "visually_reviewed")
        Subtext("Vídeo associado pelo título ou descrição; técnica ainda pendente de revisão visual.")
    // Modo experimental: WebView em tela, preservado para diagnóstico.
    // Uma página HTML com base HTTPS fornece a origem (Referer) do aplicativo.
    // Algumas instalações Android falham neste modo; Custom Tabs é o padrão.
    if (playbackError == null) {
        key(video.exerciseId, video.youtubeId, video.startSeconds, video.endSeconds, reloadCount) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                factory = { appContext ->
                    WebView(appContext).also { view ->
                        view.setBackgroundColor(Color.BLACK)
                        view.settings.javaScriptEnabled = true
                        view.settings.domStorageEnabled = true
                        view.settings.mediaPlaybackRequiresUserGesture = true
                        view.settings.allowFileAccess = false
                        view.settings.allowContentAccess = false
                        view.webChromeClient = WebChromeClient()
                        view.webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                            ) {
                                if (request != null && error != null &&
                                    (request.isForMainFrame || YoutubeEmbedPage.isEmbedRequest(request.url.toString()))
                                ) {
                                    val detail = "Erro WebView ${error.errorCode}: ${error.description}"
                                    Log.e("MeuTreinoVideos", "$detail (${request.url})")
                                    playbackError = detail
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
                            ) {
                                if (request != null && errorResponse != null &&
                                    YoutubeEmbedPage.isEmbedRequest(request.url.toString())
                                ) {
                                    val detail = "Erro HTTP ${errorResponse.statusCode} ao carregar o YouTube"
                                    Log.e("MeuTreinoVideos", "$detail (${request.url})")
                                    playbackError = detail
                                }
                            }
                        }
                        view.loadDataWithBaseURL(
                            YoutubeEmbedPage.baseUrl(appContext.packageName),
                            YoutubeEmbedPage.html(video.embedUrl),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.destroy()
                }
            )
        }
    } else {
        Subtext("Não foi possível abrir o player incorporado neste dispositivo.")
        Subtext("Diagnóstico: ${playbackError}")
        OutlineButton("Tentar novamente", onClick = {
            playbackError = null
            reloadCount += 1
        })
    }
    OutlineButton("Abrir original no YouTube", onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
    })
    AdditionalVideoOptions(exerciseId)
    OutlineButton("Fechar demonstração", onClick = { open = false; playbackError = null })
    Subtext("Se o player incorporado falhar, use 'Abrir original no YouTube'. Requer internet e WebView atualizado.")
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


/** Abre o player oficial em aba Custom Tabs com a identidade do aplicativo.
 * Técnica descrita pelo YouTube: Intent.EXTRA_REFERRER com android-app://<package>.
 * Se não houver navegador adequado, retorna ao link original, já testado no celular.
 */
private fun openEmbeddedInCustomTab(context: Context, embedUrl: String, watchUrl: String) {
    try {
        val tab = CustomTabsIntent.Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
        tab.intent.putExtra(
            Intent.EXTRA_REFERRER, Uri.parse("android-app://${context.packageName}")
        )
        tab.launchUrl(context, Uri.parse(embedUrl))
    } catch (error: Exception) {
        Log.w("MeuTreinoVideos", "Custom Tabs não abriu; usando YouTube original", error)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl)))
        } catch (fallbackError: ActivityNotFoundException) {
            Log.e("MeuTreinoVideos", "Nenhum aplicativo disponível para visualizar o vídeo", fallbackError)
            Toast.makeText(context, "Não há navegador ou YouTube disponível para abrir o vídeo.", Toast.LENGTH_LONG).show()
        }
    }
}

private fun formatVideoTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
