# Meu Treino — notas da versão 0.3.3

## Alterações visuais

1. Na aba **Dispositivos → Painel**, o cartão **Última consulta ao Health Connect** passou do topo para o **final absoluto do painel**, depois de sessões importadas. Continua exibindo a data/hora da consulta concluída pelo Meu Treino, e não necessariamente da sincronização da pulseira com o Zepp Life.
2. O gráfico de **Frequência cardíaca** ganhou eixo vertical numérico em **bpm**, com escala automática.
3. O gráfico de **Sono e recuperação** ganhou eixo vertical numérico em **horas**, com escala a partir de zero.
4. O gráfico do **IAA** ganhou eixo vertical numérico em **pontos**, cobrindo as curvas local e integrada.
5. Os rótulos horizontais dos **sete dias** foram preservados e alinhados à área de desenho dos gráficos (desconsiderando a margem dos números do eixo vertical).
6. Medições inexistentes continuam representadas por lacunas; não há preenchimento artificial dos dias sem registros.

## Itens preservados

- Permissões, leitura e integração do Health Connect, Mi Band 7 e balança Xiaomi.
- Fórmulas e salvamento dos dados do IAA, cálculo de cardio, planejamento de musculação.
- Banco Room na versão 3; nenhuma migração de dados é necessária.
- Biblioteca de exercícios (ampliação para ≥150 exercícios, incluindo calistenia, na v0.4.0).

## Versão e testes

- `versionName = "0.3.3"`; `versionCode = 6`.
- GitHub Actions: artifact `MeuTreino-Android-v0.3.3-debug`.
- Teste unitário `ChartAxisTest.kt` e smoke test independente do Android `tests/ChartAxisSmokeTest.kt` validam limites e marcas dos eixos.
- A compilação completa do APK precisa passar pelo GitHub Actions antes da instalação.

## Atualizar via ZIP de arquivos alterados

Na raiz do repositório Git local (`~/muscule-mass`):

```bash
unzip -o ~/Downloads/MeuTreinoAndroid_v0.3.3_arquivos_alterados.zip -d .
git add -A
git commit -m "Meu Treino v0.3.3 - graficos e ordem do painel"
git push
```
