**Status v0.4.10:** reprodução padrão via Custom Tabs, player WebView experimental; validação real pendente.

# Estado do Meu Treino — 20/10/2026

- v0.1.1: primeira versão compilada e instalada.
- v0.2.0: usuário validou o planejador e a separação do tempo de cardio e musculação.
- v0.3.0–0.3.1: Mi Band 7 (conexão e bateria), balança Xiaomi (peso) e leitura via Health Connect validados no celular.
- v0.3.2: usuário confirmou compilação e funcionamento do painel de saúde/IAA integrado.
- v0.3.3: usuário confirmou funcionamento dos gráficos com eixos numéricos e horário de sincronização no rodapé do painel.
- **v0.4.0 (código preparado):** 234 exercícios (91 de calistenia), biblioteca consultável, critérios de substituição revisados e nova seleção para treinos alternados. Ainda sem validação da compilação Android e do uso real no celular.

As integrações com dispositivos, IAA e banco Room v3 foram preservadas; não há migração de dados na v0.4.0. A versão não representa prescrição médica individual nem pontuação PAI oficial Xiaomi.

- **v0.4.7 (08/11/2026, código preparado):** importados os 129 vídeos selecionados na revisão final, mantendo os 104 anteriores; 233/234 exercícios com player. Foram catalogadas 172 alternativas **não revisadas** para abertura externa. Uma busca permanece devido à rejeição do único vídeo proposto. Validação estrutural local: OK. A compilação Android e teste real no aparelho ainda estão pendentes.

- **v0.4.8 (08/11/2026, hotfix de UI):** inclui explicitamente `ExerciseLibraryPanel.kt` no pacote de alterações para corrigir a tela legada que ainda mostrava “Vídeo específico ainda não verificado.” apesar do catálogo JSON v0.4.7. O validador de CI agora testa também a ligação da tela ao player. Versão 0.4.8 (`versionCode=15`); compilação e teste no aparelho pendentes.

- **v0.4.9 (08/11/2026, hotfix WebView):** player incorporado via HTML com origem HTTPS para melhorar identificação/Referer, diagnóstico de erros WebView e fallback YouTube preservado. Validação do player no celular pendente. Nenhuma migração de dados.

- **v0.4.10 (validado no celular pelo usuário):** acesso ao YouTube funciona; o WebView experimental apresentava `net::ERR_CACHE_MISS` e foi removido da interface na atualização seguinte.
- **v0.5.0 (código preparado):** player simplificado para um único botão de acesso ao YouTube; novas páginas Histórico → Evolução/Medidas/Fichas; migração Room 3→4 preservando os dados; snapshots das fichas e contagens reais. Testes de Kotlin puro e SQL local passaram; compilação APK e validação em telefone dependem do GitHub Actions e da instalação pelo usuário. Consulte `docs/ALTERACOES_v0.5.0.md`.
- **Próxima v0.6.0:** motor de reavaliação/variação inteligente da ficha com análise multifatorial e gatilho experimental de 80%, conforme especificação v2. **Ainda não implementado.**
