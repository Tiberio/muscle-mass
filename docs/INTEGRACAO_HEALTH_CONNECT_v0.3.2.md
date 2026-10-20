# Meu Treino v0.3.2 — Painel de dados e integração do IAA

Data de preparação: 20/10/2026.

## O que mudou

1. A aba **Dispositivos → Painel** oferece cards e gráficos coloridos para **passos por dia**, **frequência cardíaca média das amostras por dia**, **sono registrado por dia** e **saldo do IAA (local × estimado com Health Connect)** nos últimos sete dias. As métricas também estão em texto acessível.
2. Os registros de FC são lidos com **paginação** e reduzidos a uma amostra por minuto e origem para o painel; hora e app de origem são preservados. A leitura não é de tempo real. Exibe aviso quando algum limite de consulta é atingido.
3. As sessões de exercício mantêm início, término, origem e identificador; dados detalhados permitem verificar sua coincidência temporal. Nenhuma leitura obriga Zepp Life / Mi Fitness a sincronizar a pulseira.
4. Os passos continuam consultados por **agregação Health Connect**, em vez de somar ingenuamente fontes como celular, Mi Fitness e Google Fit.
5. **IAA local:** permanece exatamente como antes, baseado em cardio e séries realmente registrados no Meu Treino. **IAA integrado (experimental):** soma, temporariamente, pontos atribuídos a sessões externas que cumpram regras conservadoras. As sessões externas NÃO são persistidas no banco local e o histórico manual não é alterado.

## Regra aberta de estimativa de cardio do Health Connect

- Precisa de uma sessão de exercício identificada e com duração de **5 minutos a 6 horas**.
- Precisa de idade válida (18–100 anos) para estimar `FCmax ≈ 220 − idade`. É uma estimativa populacional, não medição clínica individual.
- FC precisa ter leituras em **ao menos 60% dos minutos** da sessão e em pelo menos cinco minutos distintos. Minutos sem leituras não recebem pontos e não são preenchidos artificialmente.
- Em cada minuto com amostras disponíveis, calcula-se a média; `50–70% FCmax` é classificado moderado (2/3 ponto/min) e `≥70%` vigoroso (4/3 ponto/min). Leitura abaixo de 50% não pontua. Fatores como medicamentos, calor, movimentos do punho e condicionamento interferem na precisão: **não é prescrição nem avaliação médica**.
- Sessões Health Connect com sobreposição de 50% ou mais entre si são consideradas possíveis duplicatas; somente uma é considerada. Se já houver um registro de cardio no Meu Treino no mesmo dia, a sessão externa **não** recebe pontos, para evitar duplicação. Esse critério conservador pode deixar de contar atividades realmente distintas no mesmo dia; isso será revisado em uma futura versão com vínculo explícito por sessão.
- Não há pontuação automática derivada apenas dos passos, de uma amostra isolada de FC, de peso ou de sono.
- O saldo **IAA integrado é exibido no Painel, separado do IAA local**; não representa o PAI original da Xiaomi e não é índice clínico validado.
- Nenhuma FC é enviada à Xiaomi ou ao Health Connect pelo aplicativo; são leituras locais, autorizadas. Revogar permissão em Configurações interrompe as novas leituras.

## Proveniência, atualização e uso

1. Abra o Zepp Life e aguarde a sincronização da Mi Band 7. Conforme a configuração, os registros poderão ser gravados no Health Connect por apps intermediários.
2. Conceda ao **Meu Treino** permissões de leitura para passos, peso, frequência cardíaca, exercícios e, opcionalmente, sono.
3. Abra **Dispositivos → Painel → Atualizar leitura**. O horário da consulta e os horários das medições são coisas diferentes; o app não controla quando Zepp Life envia seus dados.
4. Verifique o app de origem de cada sessão. Esse app de origem não comprova que o dado veio da Mi Band 7.
5. Se não houver amostras cardíacas suficientes ou sessões de exercício no Health Connect, o IAA integrado ficará igual ao local (sem pontuação inventada).

## Limitações e próximos passos

- Um relógio pode gerar FC em intervalos maiores que um minuto; nesses casos, a regra de cobertura pode deixar muitas sessões sem pontuação.
- A média diária representa apenas o conjunto de **amostras disponibilizadas** pelos aplicativos, não a FC de repouso nem o esforço global do dia.
- A classificação por percentual de FC máxima é uma aproximação; a ACSM/OMS não validou o IAA, que é uma métrica própria.
- O código preserva a base Room versão 3 e os dados da v0.3.1. Não foi necessário adicionar migração.
- A compilação Android e o teste no celular devem ser realizados no GitHub Actions e no aparelho do usuário.
- A v0.4.0 está reservada para o catálogo de **no mínimo 150 exercícios incluindo calistenia**, não para ampliar esta integração.

## Referências

- Android Developers, *Read raw data*: https://developer.android.com/health-and-fitness/health-connect/read-data
- Android Developers, *Read aggregated data*: https://developer.android.com/health-and-fitness/health-connect/aggregate-data
- Android Developers, *Health Connect records*: https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/HeartRateRecord
- American Heart Association, *Target Heart Rates*: https://www.heart.org/en/healthy-living/exercise-and-physical-activity
- OMS, *Guidelines on physical activity and sedentary behaviour*: https://www.who.int/publications/i/item/9789240015128
