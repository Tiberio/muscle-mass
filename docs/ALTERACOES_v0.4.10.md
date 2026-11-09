# Meu Treino v0.4.10 — caminho alternativo para demonstrações de vídeos

## Motivo

Na v0.4.9 o botão de demonstração acessava um iframe do YouTube via Android WebView, mas a reprodução foi bloqueada no dispositivo de teste. A mensagem genérica indica erro do WebView; ainda é necessário obter a linha de diagnóstico ou logcat para determinar se é rede, HTTP, origem ou outra restrição.

## Modificações

- O botão **▶ Ver demonstração** agora abre o embed oficial do YouTube em uma aba do navegador (**Android Custom Tabs**), com `Intent.EXTRA_REFERRER=android-app://br.com.meutreino`, conforme orientação oficial do YouTube para esse modo. O usuário pode retornar facilmente ao aplicativo.
- Mantido o botão **Abrir original no YouTube**, confirmado funcional no celular. Se Custom Tabs falhar ao iniciar, tenta abrir automaticamente esse original.
- O antigo WebView aparece somente em **Testar player incorporado (experimental)** e preserva seu diagnóstico `Erro WebView`/`Erro HTTP`.
- Conservados os 233 vídeos principais, 172 alternativas e 43 trechos, sem mudanças no catálogo nem nas medidas e treinos.
- Se o vídeo possui trecho, o app mostra tempos inicial e final para referência. Abrir a versão original no YouTube normalmente respeita o início, mas pode não limitar o encerramento.
- Validação estrutural ampliada para impedir que o botão principal volte a usar o WebView defeituoso.

## Validação

1. Executar `python3 scripts/validate_exercise_videos.py` (somente testes estáticos).
2. Compilar `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions.
3. No celular, abrir **Plano → Explorar biblioteca → Flexão de braços → Ver orientações → ▶ Ver demonstração**; confirmar exibição em aba de navegador.
4. Se a aba apresentar erro, testar **Abrir original no YouTube**. O YouTube pode bloquear algumas incorporações individualmente.
5. Opcionalmente testar o player incorporado experimental e anotar a linha exata **Diagnóstico:**.

**Limitação:** não foi possível testar a aba Custom Tabs em Android real neste ambiente. Uma compilação bem-sucedida não comprova reprodução; o caminho externo oficial é o único já confirmado no dispositivo do usuário.

Referência: https://developers.google.com/youtube/terms/required-minimum-functionality (seção Mobile app with player inside a native browser tab).
