# Meu Treino Android — v0.6.0

**Base:** v0.5.0, confirmada funcional no aparelho pelo usuário. `versionName=0.6.0` e `versionCode=19`.

## Funcionalidades implementadas

- **Plano → Solicitar variação da ficha:** avalia a versão vigente por tempo desde o registro, sessões concluídas **com séries efetivas**, exposições reais por grupo muscular, indícios limitados de progresso em carga/repetições, último check-in voluntário e metas explícitas de circunferência.
- **Limiar experimental:** 80% em QUALQUER dos indicadores de tempo, sessões ou exposições inicia reavaliação; não recomenda automaticamente troca. Padrões ajustáveis na tela: janela de 6, 8 ou 10 semanas; 8, 10 ou 12 exposições por grupo; antecipação 75%, 80% ou 90%. Meta de sessões deriva do número de dias semanais de musculação.
- **Proposta segura e pontual:** até uma substituição por decisão, mantendo padrão do movimento, músculo principal, equipamentos cadastrados e nível informado; prévia com exercício atual e proposto. Nenhuma alteração automática de cardio, volume ou dias. Caso não exista equivalente, nenhuma troca é oferecida.
- **Condição desfavorável:** primeira decisão consciente ao pedir troca mesmo sem recomendação, seguida de uma **segunda confirmação explícita**, prévia da proposta e **confirmação final de aplicação**. Cancelamento conserva ficha e contadores.
- **Dor persistente ou equipamento indisponível:** bloqueia troca automática; orienta reavaliar as condições e/ou buscar profissional, sem usar troca de exercícios como tratamento.
- **Registro de recuperação:** check-in voluntário (bem, cansado, dor persistente); registros de até 14 dias são considerados na avaliação; ausentes ou antigos são rotulados indisponíveis.
- **Metas corporais:** na aba Histórico → Medidas, permite criar meta individual a partir de uma medição anterior, registrar progresso percentual da **variação** desejada e excluir a meta; ausência de meta não é interpretada como fracasso.
- **Auditoria e versões:** adição das tabelas `plan_review_events`, `recovery_checkins`, `body_measurement_goals`, preservando sessões e versões da v0.5.0. Ao confirmar troca, cria snapshot imutável (ou reutiliza se já existir) e registra motivo e parecer. Sessões novas ficam vinculadas à versão correspondente.
- **Correção estrutural de substituições:** Plano/Hoje/Execução e o fingerprint das versões passam a usar o exercício efetivamente selecionado. Séries novas guardam o ID do exercício realmente realizado, não o ID basal.
- **Aviso não intrusivo:** a aba Plano sinaliza quando pelo menos um dos marcos antecipados foi alcançado; não altera a ficha.

## Migração de dados

Room 4→5, somente aditiva: cria três tabelas, dois índices; preserva `sessions`, `sets`, `weight_measurements`, `cardio_activities`, `body_measurements` e `plan_versions`. As preferências novas usam padrões de compatibilidade do DataStore. Não há migração destrutiva nem necessidade de reinstalação.

**Atualize o APK sem desinstalar o anterior.** APKs debug do GitHub Actions podem variar a assinatura, impedindo atualização; não desinstale nem apague dados antes de assegurar backup.

## Limitações explícitas

- A análise **não** equivale a avaliação fisiológica, diagnóstico médico ou prescrição por entidades de saúde. Limiar 80%, semanas e exposições são parâmetros experimentais de produto.
- A detecção de progresso analisa apenas séries comparáveis com histórico; não infere técnica, qualidade da execução, RPE, hipertrofia, diagnóstico de platô ou segurança individual.
- O parecer pode recomendar revisão/manutenção com dados insuficientes. Recomendar variação pontual só quando há solicitação por estagnação, janela antecipada e histórico mínimo estável sem relato de cansaço/dor; ainda é uma sugestão conservadora.
- A v0.6.0 **não** gera uma ficha inteiramente nova, nem altera cargas/volume automaticamente. Trata variação pontual equivalente e preserva a decisão do usuário.
- Tempo desde a ficha é medido pelo timestamp de criação do snapshot. Reativar um snapshot antigo pode requerer um registro separado de reativação numa versão futura.
- A coluna `planVersionId` da v0.5.0 era nula em sessões legadas; não associar arbitrariamente essas sessões a fichas históricas. A migração conserva esse critério.
- Os locais de medida anteriores usam nomes históricos como “Bíceps direito (braço contraído)” para circunferência do braço. Não se mede uma circunferência isolada do músculo bíceps; nomes serão normalizados com migração cuidadosa posteriormente, sem descartar medições.
- Salvar preferências em DataStore e auditoria no Room são operações consecutivas, não uma transação distribuída. Em falha abrupta entre as gravações, a versão efetiva pode estar correta sem evento de auditoria. Um mecanismo de reconciliação pode ser acrescentado.
- Os testes locais de Kotlin puro, SQLite e ligações estáticas de UI passaram; compilação completa depende de GitHub Actions e testes no celular ainda não foram executados nesta entrega.

## Roteiro de teste no celular

1. Instale **por cima** da 0.5.0. Confirme Histórico → Sessões, Evolução, Medidas, Fichas e vídeos no YouTube.
2. Plano → Solicitar variação: com ficha recente e poucas sessões, deve indicar que a troca não é recomendada, mostrar **duas confirmações antes da prévia**, e solicitar aprovação final. Cancele e confirme que a ficha não mudou.
3. Selecione variação compatível, veja o antes/depois, aplique; confira novo snapshot em Histórico → Fichas e a mudança na execução do treino (não apenas no rótulo do Plano).
4. Em Histórico → Medidas, crie uma meta com valor inicial existente, registre segunda medida, confira progresso da mudança desejada.
5. Em Revisão, selecione “Cansado” ou “Dor persistente”, verifique aviso de cautela; com dor persistente a alteração automática deve ser bloqueada.
6. Confira diferença entre **tempo**, **sessões efetivas** e **exposições por grupo** e ajuste o limite de 80% para observar o aviso antecipado (sem alteração automática).
7. Verifique persistência após fechar e reabrir o aplicativo. Sem sessão ativa, confirme que as fichas antigas permanecem disponíveis.


## Correção de compilação (09/11/2026)

- Eliminada importação duplicada de `ExerciseCatalog` em `MainViewModel.kt`, capaz de causar `conflicting import` no compilador Kotlin.
- Adicionado `scripts/check_kotlin_imports.py` e nova etapa preventiva no GitHub Actions.
- Ajustado o comando Gradle de CI para `--console=plain` sem `--stacktrace` por padrão, para facilitar a identificação de linhas de erro `e:` em futuras falhas.
- `versionName=0.6.0` e `versionCode=19` permanecem inalterados; a versão anterior não chegou a gerar APK. Não há migração nova nem alteração no formato do banco.
- Validação completa do APK Android continua dependente da execução do GitHub Actions.
