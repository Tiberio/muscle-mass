# Meu Treino — Android v0.3.3

Projeto Android **Kotlin + Jetpack Compose + Room + DataStore**, com interface escura, planejador de musculação e cardio, registro de sessões e índice de atividade semanal **IAA**. Os tempos de musculação e cardio continuam **independentes**.

**Base v0.3.2:** painel colorido (passos, frequência cardíaca, sono e IAA) e leitura detalhada dos últimos 7 dias pelo Health Connect. Estimativa integrada do IAA a partir de sessões de exercício com amostras suficientes de FC, sem copiar dados externos para o banco; prioriza impedir duplicações. Mantidas as integrações Bluetooth com Mi Body Composition Scale 2 e diagnóstico da Mi Band 7 (bateria) existentes na v0.3.1.

**Novidade v0.3.3:** alteração exclusivamente visual do Painel da aba Dispositivos: o cartão da última consulta ao Health Connect agora é o **último quadro**, após as informações e gráficos; frequência cardíaca, sono e IAA passaram a apresentar **escala vertical numérica** (bpm, horas e pontos), preservando os rótulos horizontais dos dias. Não alteramos fórmula do IAA, sincronização, permissões, catálogo ou banco de dados.

[Alterações e instruções da v0.3.3](docs/ALTERACOES_v0.3.3.md)

[Notas técnicas, método, limites e testes da v0.3.2](docs/INTEGRACAO_HEALTH_CONNECT_v0.3.2.md)

- [Integração, permissões, testes e limitações](docs/INTEGRACAO_DISPOSITIVOS_v0.3.0.md)
- [Fundamentação científica](docs/FUNDAMENTACAO_CIENTIFICA.md)
- [Histórico das mudanças v0.2.0](docs/ALTERACOES_v0.2.0.md)
- [Próxima v0.4.0: ≥150 exercícios, incluindo calistenia](docs/PLANEJAMENTO_v0.4.0.md)

## O que funciona no código

- Configuração inicial em oito etapas, perfil e preferências locais.
- Ambientes múltiplos; escolha do ambiente padrão e substituição temporária por sessão.
- Plano de treino por dias, alternância de grupos musculares, cardio diário quando solicitado.
- Musculação e cardio com durações **independentes**.
- Catálogo inicial de exercícios, ainda pequeno (expansão na v0.4.0).
- Registro de séries, cargas, repetições, RIR, histórico e exclusão de sessões.
- IAA de sete dias com ganho/expiração diária: valor local preservado e estimativa integrada adicional de sessões externas com FC suficiente.
- Leitura de Health Connect ao abrir Dispositivos e sob demanda, com permissão Android: passos agregados, evolução diária de FC, sessões, peso e sono, quando presentes.
- Busca Bluetooth por peso estabilizado da Mi Body Composition Scale 2, com confirmação manual antes de salvar.
- Pesagens confirmadas salvas em banco Room com origem informada; botão **Importar da balança** copia último peso confirmado ao perfil.
- Banco `Room v3` com migração explícita `1→2→3`, evitando apagar históricos.

## Limitações importantes

- **A v0.3.3 não foi compilada nem executada no Android neste ambiente de elaboração.** A compilação completa será validada pelo GitHub Actions; a v0.3.2 já havia sido validada no celular.
- Bluetooth com Mi Body Composition Scale 2 usa protocolo experimental. O usuário já confirmou leitura de pesagem estável no aparelho real; compatibilidade com outras unidades não é garantida.
- Health Connect não é a Mi Band 7. A presença de um dispositivo no cadastro **não comprova** que o app do fabricante envia seus dados ao Health Connect.
- O **IAA local** não importa cardio externo. O **IAA integrado experimental** no Painel calcula pontuação apenas para sessões externas que tenham FC suficiente, sem sobreposição com cardio local; passos e peso não pontuam.
- Não há leitura do PAI original Xiaomi; o IAA é um indicador próprio, não clínico.
- Percentuais de gordura, composição corporal e impedância da balança não são calculados; importamos apenas peso estabilizado.
- Não há nuvem própria, conta, telemetria nem leitura em segundo plano.
- O catálogo de pelo menos 150 exercícios e calistenia é **escopo da v0.4.0**, não desta.

## Compilação no GitHub Actions

1. Envie os arquivos ao repositório já existente com `git add -A`, `git commit` e `git push`.
2. Abra **Actions → Compilar APK Android** e acompanhe a execução.
3. Após sucesso, baixe o artifact **`MeuTreino-Android-v0.3.3-debug`**.
4. Extraia `app-debug.apk` do ZIP e instale-o.

O GitHub usa JDK 21, Gradle 9.4.1, Android SDK 37 e Build Tools 36.0.0, preservando sua preferência por não instalar essas ferramentas manualmente no Debian.

**Aviso de dados:** APKs de depuração gerados por trabalhos diferentes podem utilizar chaves de assinatura diferentes. Se `adb install -r app-debug.apk` acusar `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, não desinstale a versão antiga precipitadamente, pois os dados podem ser perdidos. Um processo de assinatura persistente e backup voluntário é uma melhoria futura.

## Testes e desenvolvimento local

Para os testes independentes do Android SDK, com compilador Kotlin instalado:

```bash
kotlinc app/src/main/java/br/com/meutreino/devices/MiScaleParser.kt tests/MiScaleParserSmokeTest.kt -include-runtime -d mi-scale-test.jar
java -jar mi-scale-test.jar
```

A integração Jetpack/Android e o banco Room são testados pela compilação e por testes no celular após `git push`. Ver roteiro prático em `docs/INTEGRACAO_DISPOSITIVOS_v0.3.0.md`.

## Estrutura simplificada

```text
app/src/main/java/br/com/meutreino/
  MainActivity.kt  MainViewModel.kt  PermissionsRationaleActivity.kt
  devices/HealthConnectGateway.kt MiScaleScanner.kt MiScaleParser.kt
  data/WorkoutDatabase.kt SettingsStore.kt
  domain/{ActivityScore,ActivityEvents,TrainingPlanner,...}.kt
  ui/{AppRoot,HistoryDevicesProfile,WizardScreen,...}.kt
app/src/test/java/br/com/meutreino/devices/MiScaleParserTest.kt

docs/FUNDAMENTACAO_CIENTIFICA.md
docs/INTEGRACAO_DISPOSITIVOS_v0.3.0.md
docs/PLANEJAMENTO_v0.4.0.md
```

## Próxima atualização: v0.4.0

Ampliação do catálogo para **≥150 exercícios distintos**, com calistenia, pesos livres, máquinas e elásticos. Cada registro terá grupos musculares, equipamentos, dificuldade e orientações claras, com testes de cobertura. As funcionalidades de dispositivos já introduzidas serão mantidas e corrigidas conforme os testes da v0.3.2.

### Mi Band 7 — diagnóstico BLE experimental (v0.3.0)

Em **Dispositivos → Conexões → Mi Band 7**, é possível procurar dispositivos BLE, testar a conexão GATT e verificar serviços padrão de bateria/FC. Isso **não é pareamento Huami nem acesso ao histórico**. Consulte `docs/MI_BAND_7_v0.3.0.md`.
