# Pesquisa da biblioteca de vídeos — 161 exercícios pendentes

**Esta entrega é um pacote de pesquisa; não altera o aplicativo Android.**

A base consolidada contém **73 vínculos anteriores** e 161 IDs pendentes. Nesta rodada foram conferidos os metadados de mais **13 vínculos** diretamente no JSON público do Workout Database. Logo, o relatório parcial tem **86 vínculos identificados em metadados** e **148 pendentes**. *Identificado* não significa que o vídeo foi assistido ou que o recorte foi conferido.

## Execução completa de todos os 161 IDs no GitHub Actions

O pacote inclui o script `pesquisa_videos/pesquisar_161.py`, um arquivo com os 161 IDs e nomes em inglês, os 73 vínculos anteriores, 13 novos candidatos consultados e um workflow `.github/workflows/pesquisa-videos-161.yml`.

1. Descompacte na raiz de seu repositório `muscule-mass`.
2. Faça `git add pesquisa_videos .github/workflows/pesquisa-videos-161.yml`, `git commit -m "Pesquisa 161 videos de exercicios"` e `git push`.
3. O workflow **Pesquisa dos 161 videos pendentes** executará automaticamente ou poderá ser iniciado em **Actions → Run workflow**.
4. Ao terminar, baixe o artifact `Pesquisa-161-exercicios-WorkoutDB`, que contém `RELATORIO_161.md`, `RESULTADOS_161.json` e `RESULTADOS_161.csv`. **Esse relatório efetivamente cruza os 161 IDs com o banco completo**, distingue candidatos fortes, ambíguos e ausentes e registra duração publicada quando houver.

**Não é necessário instalar dependências ou SDK Android para a pesquisa.** A etapa exige apenas Python 3.11 e acesso do GitHub Actions à fonte pública.

### Limites importantes

- O algoritmo não afirma equivalência de variantes ou de técnica sem inspeção visual. Vídeos identificados por metadados continuam sujeitos a conferência.
- Se vídeo tiver duração publicada de até 40 s, ficará marcado como candidato a reprodução integral; não haverá corte automático inventado.
- Para vídeo de mais de 40 s ou duração desconhecida, a saída marca os segundos de corte como pendentes de auditoria.
- O workflow só produz arquivos de pesquisa. **Não altera os vídeos usados no APK** e não substitui a versão v0.4.4.

Fonte principal: https://github.com/rthepen/workout-database
