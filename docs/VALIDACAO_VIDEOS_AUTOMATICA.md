# Como executar a validação automática dos 60 vídeos da v0.4.5

Esta ferramenta é **independente da compilação do APK**. Ela lê a lista `candidates` no `app/src/main/assets/exercise_videos.json` e consulta apenas os metadados oficiais da YouTube Data API v3. Não baixa vídeos nem edita o catálogo.

## Preparação (uma única vez)

1. No Google Cloud Console, crie ou selecione um projeto e ative **YouTube Data API v3** em **APIs e serviços → Biblioteca**.
2. Em **APIs e serviços → Credenciais**, crie uma chave de API, restrinja-a à **YouTube Data API v3** e guarde-a com segurança.
3. No GitHub do Meu Treino, vá a **Settings → Secrets and variables → Actions → New repository secret** e crie `YOUTUBE_API_KEY`, colando o valor da chave.
4. Copie os arquivos deste pacote para a raiz da cópia local do repositório, mantendo os diretórios `scripts/`, `.github/workflows/` e `docs/`. Faça commit e push.
5. No GitHub, abra **Actions → Validar videos candidatos YouTube → Run workflow**.
6. Após a conclusão, baixe o artifact **Auditoria-60-videos-MeuTreino**, contendo `RELATORIO_VALIDACAO.md` e `resultado_validacao.json`.

## Interpretação

- `CURTO_APTO_A_REVISAO_VISUAL`: vídeo existe, permite embed, está disponível no Brasil e dura no máximo 40 segundos. **A técnica ainda precisa de conferência visual.**
- `PRECISA_RECORTE_E_REVISAO_VISUAL`: vídeo existe e permite embed, mas dura mais de 40 segundos; escolher visualmente um trecho completo com 2 a 4 segundos de margem em cada extremidade.
- `PROBLEMA_TECNICO`: vídeo removido/privado, não incorporável, bloqueado no Brasil ou duração indisponível; substituir ou investigar.

O relatório aponta também títulos muito diferentes, mas esse alerta é apenas indicativo — o nome em português pode ser diferente do título em inglês. **Nunca promover automaticamente candidatos ao player como `approved`: metadados não verificam a variante nem a execução técnica.**

## Limites

A API do YouTube não retorna automaticamente os segundos em que uma pessoa executa corretamente um movimento, nem determina a adequação biomecânica. A seleção precisa dos trechos e a avaliação da técnica precisam de revisão visual, usando o player oficial. Não use ferramentas para baixar vídeos do YouTube sem a autorização apropriada.

Documentação:
- https://developers.google.com/youtube/v3/docs/videos/list
- https://developers.google.com/youtube/player_parameters
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets
