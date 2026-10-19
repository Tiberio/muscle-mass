# Meu Treino — projeto Android v0.2.0

**Novidade:** `docs/FUNDAMENTACAO_CIENTIFICA.md` (diretrizes médicas, descanso, cardio e fórmula do Índice de Atividade Acumulada).

**Alterações e cuidados ao atualizar:** `docs/ALTERACOES_v0.2.0.md`.

**Compilação:** pelo GitHub Actions existente, em `.github/workflows/android-debug.yml`. Esta versão ainda precisa passar pela compilação Android real e testes no celular.

---

# Meu Treino — Android (versão inicial 0.1)

Projeto Android em **Kotlin + Jetpack Compose**, com tema Moderno Escuro, criado a partir da especificação funcional aprovada.

**Situação atual:** o código-fonte foi implementado; a lógica de planejamento foi compilada e passou nos testes Kotlin puros. **O aplicativo Android completo não foi compilado ou executado em emulador nesta entrega**, pois o ambiente de geração não dispõe do SDK Android nem consegue baixar Gradle e dependências. Não há APK validado nesta pasta.

## Funcionalidades já codificadas

- Primeira utilização em 8 etapas (subtelas curtas); revisão final única e rolável; botão Editar volta diretamente à pergunta e permite retornar à revisão.
- Perfil físico, objetivo, regiões prioritárias, ambientes múltiplos, **ambiente padrão salvo**, equipamentos, rotina, 20/30/45/60 minutos ou Outro tempo, cardio e dispositivos declarados.
- Planejador determinístico local inicial, explicações e alertas para ausência de puxadas e desequilíbrio de séries.
- Proposta semanal, aceite do plano, cinco abas (Hoje, Plano, Histórico, Dispositivos, Perfil).
- Troca do ambiente apenas para a sessão atual sem modificar o ambiente padrão.
- Catálogo inicial com 23 exercícios e 1 vídeo do YouTube específico, verificado. Os demais ficam sem link em vez de abrir resultados aleatórios.
- Execução de treino, registro série a série (repetições, carga, RIR), cronômetro de intervalo e cardio informado no fechamento.
- Perfil e preferências salvos com **DataStore**; sessões e séries gravadas em **Room/SQLite**.
- Histórico real das séries e sessões cadastradas no aplicativo (não dados simulados de saúde).
- Catálogo informativo de marcas/modelos, sem afirmar conectividade que não existe.

## Ainda NÃO implementado

- Conexão Bluetooth/BLE; conexão direta com Mi Body Composition Scale 2, Mi Band 7 ou outros aparelhos.
- Leitura de dados pelo Health Connect; importação de peso/frequência cardíaca/PAI do Zepp Life.
- Busca de vídeos em massa ou catálogo completo validado; reprodução embutida de vídeos.
- Sincronização em nuvem, exportação/backup, notificações, gráficos avançados, cálculo próprio de pontos de atividade.
- Validação profissional das propostas de exercícios, progressão automática de carga e versão para Play Store.

## Requisitos de construção

- Android Studio compatível com AGP **9.2.0**, JDK 17+, **Android SDK API 37** e Build Tools 36.
- Gradle **9.4.1** (a configuração está em `gradle/wrapper/gradle-wrapper.properties`).
- Telefone Android 8.0+ (API 26+) ou emulador.
- Conexão com internet **na máquina onde o projeto será compilado**, para baixar as dependências.

### Abrir no Android Studio

1. Descompacte o projeto e escolha **File → Open** na pasta `MeuTreinoAndroid` (a pasta que contém `settings.gradle.kts`).
2. Instale o SDK API 37 no **SDK Manager** e selecione o JDK 17 ou superior.
3. Faça a sincronização do Gradle. O arquivo JAR do wrapper não acompanha esta entrega (não foi possível obtê-lo offline). Caso o IDE solicite o wrapper, execute o script abaixo para baixá-lo e gerá-lo, ou gere o wrapper com um Gradle 9.4.1 instalado.
4. Execute a configuração `app` no emulador ou telefone via cabo USB (depuração USB habilitada).

### Linux/Debian (com SDK já instalado)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"  # ajuste se necessário
bash scripts/compilar-no-linux.sh
```

O script tenta utilizar um Gradle já disponível e, caso não exista, baixa a distribuição oficial para criar o wrapper. Após compilação bem-sucedida, o APK de depuração estará em `app/build/outputs/apk/debug/app-debug.apk`.

## Testar a lógica sem o Android SDK

Os arquivos de `domain/` não dependem do Android:

```bash
kotlinc app/src/main/java/br/com/meutreino/domain/*.kt tests/PlannerSmokeTest.kt -include-runtime -d planner-tests.jar
java -jar planner-tests.jar
```

Foram executadas 10 verificações de composição do plano, respeito a equipamentos, rotina, determinismo, tempo e análise de equilíbrio.

## Organização

```text
app/src/main/java/br/com/meutreino/
  MainActivity.kt
  MainViewModel.kt
  domain/Models.kt ExerciseCatalog.kt TrainingPlanner.kt BalanceAnalyzer.kt
  data/SettingsStore.kt WorkoutDatabase.kt
  ui/Design.kt AppRoot.kt WizardScreen.kt PlanAndToday.kt WorkoutScreen.kt HistoryDevicesProfile.kt
```

## Privacidade

- Não há servidor próprio, login nem permissões de Bluetooth/Health Connect nesta versão.
- Dados de treino permanecem no armazenamento local do Android; `allowBackup=false` para não afirmar armazenamento exclusivamente local e permitir backup automático sem escolha explícita.
- A importação futura de medições exigirá consentimento separado e identificação da origem.

## Limitações técnicas conhecidas

- O aplicativo Android não foi validado em build completo nesta entrega. Compile no Android Studio e corrija eventuais incompatibilidades apontadas pela versão do IDE/SDK.
- O planejador é inicial, heurístico e não equivale a uma prescrição individual feita por profissional. Antes da execução, confira técnica, limitações e recuperação.
- Séries são salvas imediatamente; o cronômetro de descanso e o passo visual dentro da sessão ainda não são restaurados com exatidão após encerrar o processo, mas o app retoma a partir dos registros no banco.
- O catálogo inicial de vídeos está incompleto: apenas supino com halteres possui URL verificada.

## Próximo ciclo de desenvolvimento

1. Fazer o build real em Android Studio e teste em aparelho com diferentes tamanhos de tela.
2. Corrigir eventuais problemas de layout e validar retorno de edições à revisão.
3. Evoluir catálogo curado de exercícios/vídeos e validar regras de progressão/recuperação.
4. Adicionar Health Connect, medindo exatamente o que o Zepp Life ou Mi Fitness compartilham.
5. Adicionar descoberta BLE padronizada e testar especificamente a Mi Body Composition Scale 2.
6. Adicionar visualizações, exportação, backup voluntário e índice próprio de atividade.
