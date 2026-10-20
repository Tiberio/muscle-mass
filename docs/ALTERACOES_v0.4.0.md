# Meu Treino v0.4.0 — Biblioteca e planejamento

## Escopo e resultados

Esta versão amplia o catálogo anterior (23 exercícios) para **234 exercícios distintos**, dos quais **91 classificados como calistenia**, distribuídos entre movimentos com peso corporal, barras, argolas/suspensão, halteres, kettlebells, barras/anilhas, máquinas e elásticos.

Os 23 identificadores originais foram preservados para que os registros existentes do histórico continuem referenciáveis. Não há migração do banco Room nesta atualização: a estrutura permanece na versão 3.

### Modificações funcionais

- `domain/ExerciseLibraryData.kt`: 211 exercícios novos com nome, ID, padrão, músculos, requisitos de equipamento, nível e dicas de execução, sem URLs de vídeo criadas automaticamente.
- `domain/ExerciseCatalog.kt`: reúne exercícios antigos e novos; oferece filtro por ambiente/nível; acrescenta orientações sobre erros de execução e progressões/regressões verificáveis dentro do próprio catálogo; restringe substituições ao padrão e músculo principal (e, para exercícios de isolamento de braço, distingue bíceps e tríceps).
- `domain/TrainingPlanner.kt`: escolhe os exercícios por padrões e músculos, distribui superiores/inferiores em semanas com quatro ou mais sessões, prioriza alternativas não repetidas quando possível e evita completar sessões com grupos musculares incompatíveis.
- `ui/ExerciseLibraryPanel.kt` e `ui/PlanAndToday.kt`: na aba **Plano**, o botão **Explorar biblioteca** abre busca, filtros, orientações e indicação de compatibilidade com o ambiente cadastrado.
- `ui/WorkoutScreen.kt`: detalha dicas, erros comuns e progressões, além de permitir registrar carga de barra/anilhas e kettlebell.
- `ui/WizardScreen.kt`: equipamentos adicionais para casa equipada, como paralelas e suspensão/TRX.
- Versão Android `versionCode=7`, `versionName=0.4.0`; artifact `MeuTreino-Android-v0.4.0-debug`.

### Critérios técnicos

- **Troca de exercício** exige mesmo padrão de movimento e mesmo músculo principal. Para bíceps/tríceps isolados, exige ainda mesma ação. Essa é uma regra simplificada; não equivale a uma avaliação biomecânica individual.
- **Preferências e tempo**: os minutos para cardio continuam totalmente independentes dos minutos de musculação. A quantidade de exercícios permanece estimada a partir do tempo de força.
- **Descanso**: as sessões alternam a ênfase entre membros superiores/inferiores em frequências maiores, mas não substituem o julgamento sobre recuperação muscular, fadiga, dor ou lesões.
- **Vídeos**: preservamos somente links já cadastrados; os novos exercícios têm descrições próprias, sem IDs fictícios do YouTube.
- **Calistenia**: vários exercícios podem exigir barras, paralelas ou suspensão, por isso não são automaticamente classificados como executáveis sem equipamentos.

### Testes

- `app/src/test/.../ExerciseLibraryTest.kt`: contagem, unicidade, calistenia, metadados, permanência de IDs, filtros de ambiente/nível, substituição adequada, progressões válidas e cinco dias de musculação com cardio diário.
- `tests/ExerciseLibrarySmokeTest.kt`: teste Kotlin puro sem SDK Android.
- Regressões do planejador, IAA, Health Connect e parsers de dispositivo permanecem no projeto.

### Pontos para validação no celular

1. Verificar **Plano → Explorar biblioteca de 234 exercícios**. Filtrar Calistenia e alterar entre disponíveis/todos.
2. Confirmar que as fichas mudaram sem antecipar dias nem misturar tempos de cardio e musculação.
3. Testar troca de exercícios no **Plano**, especialmente rosca/tríceps e puxada/empurrada.
4. Confirmar **Dispositivos → Painel**: os gráficos da v0.3.3, o IAA e Health Connect precisam continuar idênticos.
5. Usar o APK de depuração do GitHub Actions; a assinatura poderá divergir de execuções anteriores.

### Limitações conhecidas

A biblioteca, as progressões e os avisos são **ferramentas informativas e heurísticas de software**. As sugestões não consideram contraindicações específicas nem biomecânica individual; instruções breves não substituem avaliação presencial. O sistema não estima a duração de cada exercício a partir de séries/intervalos reais nem realiza progressão automática de cargas — previstos para fase posterior. Ainda não validamos todos os exercícios com um profissional de educação física, e a compilação Android depende da execução no GitHub Actions.
