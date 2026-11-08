# Meu Treino v0.4.7 — integração da auditoria final de 130 vídeos

**Fonte:** arquivo `revisao_130_videos_meutreino (1).json`, exportado em 08/11/2026, comparado com o conjunto de opções do site de revisão existente.

## Dados integrados

- **129 aprovados, 1 rejeitado e 0 pendentes** entre os 130 exercícios da auditoria final.
- Os **104 vínculos de vídeo da v0.4.6** foram mantidos sem alterações.
- Resultado: **233 vídeos principais no player para 234 exercícios**.
- Os 129 novos vídeos possuem identificador exato selecionado, canal, título e intervalo de reprodução quando aplicável. **26 têm trechos de até 40 segundos**; os curtos continuam integrais.
- **Supino em pé nos cabos (`cablechest`)** continua sem vídeo principal, devido à rejeição registrada pelo usuário. Sua busca externa é preservada.
- O site contém **172 vídeos alternativos** para exercícios com vídeo principal já aprovado. Essas opções **não receberam aprovação visual individual**, por isso são listadas unicamente como links externos na seção expansível “Quer mais opções de vídeos?”. Nada é promovido automaticamente ao player.

## Alterações técnicas

- `assets/exercise_videos.json`: adicionados 129 itens em `videos`, 172 itens em `alternatives` e removidas 129 buscas substituídas por vídeos aprovados; permanece uma busca direcionada.
- `domain/ExerciseVideoCatalog.kt`: modelo e catálogo de links alternativos, indexados por ID de exercício, com validação de identificadores e duplicação.
- `ui/ExerciseVideoAssetRepository.kt`: carregamento local das opções externas com verificação explícita `not_visually_reviewed` e `external_only`.
- `ui/ExerciseVideoPlayer.kt`: painel expansível de alternativas, sem substituição do vídeo principal e sem reproduzir opções não revisadas no player.
- `scripts/validate_exercise_videos.py`: validação do total, fonte, metadados, trechos e cobertura completa dos 234 IDs.
- `app/build.gradle.kts`: versão 0.4.7, `versionCode=14`.
- GitHub Actions: nome do artefato atualizado para `MeuTreino-Android-v0.4.7-debug`.

## Limites e próximos passos

- A estrutura do JSON foi validada localmente; uma compilação Android completa ainda precisa ser realizada no GitHub Actions, seguida de teste manual no celular.
- Não verificamos agora se o YouTube mantém todos os vídeos disponíveis ou se cada autor permite a reprodução incorporada. Há botão para abrir o original no YouTube.
- A seleção de vídeos é a revisão feita pelo usuário, não uma certificação técnica ou médica.
- **Sem mudança** em banco de dados, dados de treino, Health Connect, integração Xiaomi, IAA, lógica A/B, datas, ambientes ou configuração inicial.
- Próximo marco de desenvolvimento: **v0.5.0 — progressão e avaliação corporal**, conforme o roadmap existente e os requisitos v2 de revisão inteligente da ficha.

## Envio ao repositório

Na raiz de `muscle-mass`, extraia o ZIP de alterações, execute `python3 scripts/validate_exercise_videos.py`, depois `git add`, `git commit` e `git push`. Acompanhe o GitHub Actions antes de instalar um APK.
