# Pesquisa dos vídeos pendentes — Meu Treino

Base: v0.4.6, com 104 exercícios cujos vídeos já foram incorporados ao aplicativo. Esta pesquisa **não modifica** esse catálogo.

## Resultado preliminar

- 130 exercícios sem vídeo aprovado (origem: `discovery` do `exercise_videos.json` da v0.4.6).
- 44 candidatos específicos identificados por cruzamento e pesquisa de fontes públicas, **ainda não validados visualmente**.
- 86 exercícios sem candidato específico escolhido; o workflow consulta os nomes em inglês na API oficial do YouTube e traz até três resultados por exercício.
- A classificação por título é apenas priorização de revisão, não aprovação técnica da execução. Algumas correspondências dos 44 candidatos representam variantes próximas e exigem atenção especial.

O levantamento completo está nos arquivos `LEVANTAMENTO_130_EXERCICIOS.md`, `.json`, `.csv`. Os candidatos conhecidos estão em `CANDIDATOS_A_REVISAR.json`; os restantes estão em `PENDENTES_SEM_LINK.json`.

### Como executar no GitHub

1. Descompacte o ZIP na **raiz do repositório** `muscle-mass`.
2. Faça commit de `.github/workflows/pesquisar-videos-130.yml`, `scripts/` e `pesquisa_videos_130/`.
3. Confirme que o secret `YOUTUBE_API_KEY` continua disponível em GitHub → Settings → Secrets and variables → Actions (já usado na auditoria anterior).
4. GitHub → Actions → **Pesquisar videos pendentes Meu Treino** → Run workflow.
5. Baixe o artifact `Pesquisa-videos-130-MeuTreino` e envie-o ao ChatGPT para seleção dos candidatos e montagem de novo revisor no GitHub Pages.

O workflow executa **86 pesquisas** `search.list`, cada uma retornando até 5 resultados de busca, e depois consulta metadados para duração, incorporação e disponibilidade no Brasil. A cota exata depende das configurações do seu projeto Google Cloud. Se houver falha de cota, o relatório identifica as consultas concluídas, as não consultadas e o motivo.

**Atenção:** uma pesquisa automática não garante que o vídeo mostre o exercício correto. Videos de mais de 40 segundos precisam de trecho escolhido e revisado. Não inclua automaticamente candidatos novos no player antes da revisão.

### Testes locais opcionais

```bash
python -m unittest discover -s scripts -p 'test_pesquisar_86_videos_youtube.py'
```
