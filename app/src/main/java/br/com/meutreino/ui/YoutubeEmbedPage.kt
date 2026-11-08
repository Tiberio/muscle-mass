package br.com.meutreino.ui

/** HTML local com base HTTPS, como recomendado para embeds do YouTube em Android WebView. */
internal object YoutubeEmbedPage {
    private val allowedUrl = Regex("https://www\\.youtube\\.com/embed/[A-Za-z0-9_-]{11}(\\?[A-Za-z0-9_=&-]+)?")

    fun baseUrl(packageName: String): String {
        require(packageName.matches(Regex("[a-zA-Z][a-zA-Z0-9_.]+")))
        return "https://$packageName/"
    }

    fun isEmbedRequest(url: String): Boolean =
        url.startsWith("https://www.youtube.com/embed/") ||
        url.startsWith("https://www.youtube-nocookie.com/embed/")

    fun html(embedUrl: String): String {
        require(allowedUrl.matches(embedUrl)) { "URL de incorporação do YouTube inválida" }
        return """<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="referrer" content="strict-origin">
<style>
 html, body {margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden;}
 iframe {display:block; width:100%; height:100%; border:0;}
</style>
</head>
<body>
<iframe src="$embedUrl" title="Demonstração de exercício"
  referrerpolicy="strict-origin"
  allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
  allowfullscreen></iframe>
</body>
</html>"""
    }
}
