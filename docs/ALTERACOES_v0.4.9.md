# Meu Treino v0.4.9 — correção experimental do player interno do YouTube

## Relato da falha

O aplicativo v0.4.8 compilou, reconheceu os 233 vídeos e apresentou **▶ Ver demonstração**, mas o player WebView exibia **Página da Web não disponível** para qualquer vídeo. **Abrir original no YouTube** funcionou.

## Causa provável e mudança

A versão anterior carregava diretamente uma URL `https://www.youtube.com/embed/...` com `Referer: android-app://br.com.meutreino`. Esse método não oferece uma origem HTTPS como a recomendada pela documentação do YouTube para uma página HTML local no Android WebView; a falha concreta de rede ainda não foi identificada por logcat.

- Substituído o carregamento direto por `WebView.loadDataWithBaseURL`, fornecendo uma origem HTTPS baseada no identificador do app, com um documento HTML contendo o `iframe` oficial do YouTube.
- Aplicado `referrerpolicy=strict-origin` no iframe, preservando os parâmetros de início/fim e a exigência de gesto do usuário.
- Detectadas falhas de rede/HTTP da URL incorporada via WebViewClient, com mensagem legível de diagnóstico, tentativa de recarga e logs `MeuTreinoVideos`.
- Preservado o botão **Abrir original no YouTube** e as 172 alternativas externas não revisadas.
- Nenhuma modificação em ficha, exercícios, dados armazenados, cadastro, saúde ou preferências.
- Atualização `versionName=0.4.9`, `versionCode=16` e validações de regressão no CI.

## Teste necessário no celular

1. Instalar o APK compilado no GitHub Actions, sobre a versão atual.
2. Plano → Explorar biblioteca → Todos → Flexão de braços → Ver orientações → ▶ Ver demonstração.
3. Tocar em reproduzir dentro do quadro. Repetir em outro vídeo, com trecho selecionado.
4. Confirmar se **Abrir original no YouTube** funciona e se a seção de alternativas aparece quando disponível.
5. Se o quadro continuar indisponível, registrar o texto de **Diagnóstico** e, se possível, extrair `adb logcat -d -s MeuTreinoVideos:E`.

**Limite:** correção baseada na documentação do YouTube; não foi possível executar o Android WebView real neste ambiente. Uma compilação bem-sucedida não prova a reprodução; pode haver restrições de WebView, rede ou incorporação específicas do aparelho.

Referência técnica: https://developers.google.com/youtube/terms/required-minimum-functionality
