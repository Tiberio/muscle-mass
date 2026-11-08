# Meu Treino v0.4.8 — correção do acesso aos vídeos na biblioteca

## Problema identificado

A atualização incremental da v0.4.7 enviada ao GitHub omitiu `app/src/main/java/br/com/meutreino/ui/ExerciseLibraryPanel.kt`. Esse arquivo permaneceu na implementação antiga que, após tocar em **Ver orientações**, mostrava `Vídeo específico ainda não verificado.` com base no campo legado `exercise.youtubeVideoId`.

A compilação foi concluída porque o arquivo antigo continuava sendo código válido. O catálogo JSON de 233 vídeos foi enviado, mas **a interface não invocava o player**.

## Correções

- Incluído explicitamente no pacote incremental `ExerciseLibraryPanel.kt`, agora com `ExerciseVideoControl(exercise.id)`.
- `scripts/validate_exercise_videos.py` valida a conexão entre a biblioteca, o player e o asset `exercise_videos.json`, falhando na CI se a implementação legada reaparecer.
- Atualização para `versionName=0.4.8` e `versionCode=15`; nome do artefato no GitHub Actions atualizado.
- Mantidos os 233 vídeos selecionados, as 172 opções externas ainda não revisadas e o exercício sem vídeo aprovado.
- Nenhuma mudança em treino, banco de dados, preferências, cadastro ou permissões.

## Teste após instalar o APK

1. **Plano → Explorar biblioteca → Todos**.
2. Buscar **Flexão de braços** ou **Supino com halteres**, tocar em **Ver orientações**.
3. Confirmar que aparece **▶ Ver demonstração**, seguido de botão externo quando há vídeo.
4. Verificar **Supino em pé nos cabos**: deve oferecer busca externa, pois o vídeo foi rejeitado.
5. Verificar o número da versão Android **0.4.8**.

**Limite:** testes estruturais locais não substituem a compilação do GitHub Actions nem a validação do funcionamento no celular e no YouTube.
