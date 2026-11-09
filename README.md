# Meu Treino — Android v0.4.9

Projeto Android **Kotlin + Jetpack Compose + Room + DataStore**, com interface escura, planejador de musculação e cardio, registro de sessões e índice de atividade semanal **IAA**. Os tempos de musculação e cardio continuam **independentes**.

**v0.4.10:** A demonstração usa por padrão uma aba de navegador (Custom Tabs), com identificação de origem exigida pelo YouTube. A reprodução no WebView está disponível apenas como teste experimental; o original no YouTube permanece acessível. Consulte [alterações v0.4.10](docs/ALTERACOES_v0.4.10.md).

**Novidade v0.4.9:** tentativa de correção de reprodução incorporada no Android WebView. A página HTML do player agora é carregada com origem HTTPS e oferece mensagens de diagnóstico e recarga. A opção externa para YouTube permanece. Consulte [alterações v0.4.9](docs/ALTERACOES_v0.4.9.md).

**Base v0.4.8:** correção de integração entre a tela **Ver orientações** e o player de vídeos. O pacote anterior não havia incluído `ExerciseLibraryPanel.kt` ao enviar arquivos alterados ao GitHub. O teste de validação agora exige a ligação da interface ao catálogo. Nenhum dado do usuário nem regra de treino foram alterados. Consulte [alterações v0.4.8](docs/ALTERACOES_v0.4.8.md).

**Base v0.4.7:** 233 de 234 exercícios possuem demonstração selecionada; 129 vídeos novos aprovados pelo usuário foram incorporados, com trechos de até 40 s. O site também listou 172 alternativas **ainda não aprovadas**, disponíveis somente como links externos na opção “Quer mais opções de vídeos?”. O exercício *Supino em pé nos cabos* permanece com busca externa após rejeição da demonstração inadequada. Consulte [alterações v0.4.7](docs/ALTERACOES_v0.4.7.md).

**Novidade v0.4.0:** biblioteca expandida para **234 exercícios**, incluindo **91 de calistenia**, com busca e filtros em **Plano → Explorar biblioteca**, instruções de execução, erros comuns, compatibilidade por ambiente/experiência, progressões documentadas e substituições mais restritas por padrão/músculo principal. Mantém dados e funcionalidades das versões anteriores. Consulte [alterações v0.4.0](docs/ALTERACOES_v0.4.0.md).

**Base v0.3.2:** painel colorido (passos, frequência cardíaca, sono e IAA) e leitura detalhada dos últimos 7 dias pelo Health Connect. Estimativa integrada do IAA a partir de sessões de exercício com amostras suficientes de FC, sem copiar dados externos para o banco; prioriza impedir duplicações. Mantidas as integrações Bluetooth com Mi Body Composition Scale 2 e diagnóstico da Mi Band 7 (bateria) existentes na v0.3.1.

**Base v0.3.3:** alteração exclusivamente visual do Painel da aba Dispositivos: o cartão da última consulta ao Health Connect agora é o **último quadro**, após as informações e gráficos; frequência cardíaca, sono e IAA passaram a apresentar **escala vertical numérica** (bpm, horas e pontos), preservando os rótulos horizontais dos dias. Não alteramos fórmula do IAA, sincronização, permissões, catálogo ou banco de dados.

[Alterações e instruções da v0.3.3](docs/ALTERACOES_v0.3.3.md)

[Notas técnicas, método, limites e testes da v0.3.2](docs/INTEGRACAO_HEALTH_CONNECT_v0.3.2.md)

- [Integração, permissões, testes e limitações](docs/INTEGRACAO_DISPOSITIVOS_v0.3.0.md)
- [Fundamentação científica](docs/FUNDAMENTACAO_CIENTIFICA.md)
- [Histórico das mudanças v0.2.0](docs/ALTERACOES_v0.2.0.md)
- [Escopo atendido na v0.4.0: 234 exercícios e calistenia](docs/PLANEJAMENTO_v0.4.0.md)

## O que funciona no código

- Configuração inicial em oito etapas, perfil e preferências locais.
- Ambientes múltiplos; escolha do ambiente padrão e substituição temporária por sessão.
- Plano de treino por dias, alternância de grupos musculares, cardio diário quando solicitado.
- Musculação e cardio com durações **independentes**.
- Catálogo com 234 exercícios e busca/filtros em Plano, preservando os 23 IDs anteriores.
- Registro de séries, cargas, repetições, RIR, histórico e exclusão de sessões.
- IAA de sete dias com ganho/expiração diária: valor local preservado e estimativa integrada adicional de sessões externas com FC suficiente.
- Leitura de Health Connect ao abrir Dispositivos e sob demanda, com permissão Android: passos agregados, evolução diária de FC, sessões, peso e sono, quando presentes.
- Busca Bluetooth por peso estabilizado da Mi Body Composition Scale 2, com confirmação manual antes de salvar.
- Pesagens confirmadas salvas em banco Room com origem informada; botão **Importar da balança** copia último peso confirmado ao perfil.
- Banco `Room v3` com migração explícita `1→2→3`, evitando apagar históricos.

## Limitações importantes

- **A v0.4.8 ainda não foi compilada nem executada no Android neste ambiente de elaboração.** A v0.4.7 compilou e foi instalada pelo usuário, porém apresentou a interface legada de vídeos devido à ausência de um arquivo na atualização enviada ao GitHub. A compilação v0.4.8 requer validação no GitHub Actions e no celular.
- Bluetooth com Mi Body Composition Scale 2 usa protocolo experimental. O usuário já confirmou leitura de pesagem estável no aparelho real; compatibilidade com outras unidades não é garantida.
- Health Connect não é a Mi Band 7. A presença de um dispositivo no cadastro **não comprova** que o app do fabricante envia seus dados ao Health Connect.
- O **IAA local** não importa cardio externo. O **IAA integrado experimental** no Painel calcula pontuação apenas para sessões externas que tenham FC suficiente, sem sobreposição com cardio local; passos e peso não pontuam.
- Não há leitura do PAI original Xiaomi; o IAA é um indicador próprio, não clínico.
- Percentuais de gordura, composição corporal e impedância da balança não são calculados; importamos apenas peso estabilizado.
- Não há nuvem própria, conta, telemetria nem leitura em segundo plano.
- O catálogo contém 234 exercícios, sendo 91 de calistenia. Há 233 vínculos de vídeo no player, dos quais 129 foram aprovados nesta auditoria; as alternativas externas não foram revisadas e precisam de conferência antes do uso.

## Compilação no GitHub Actions

1. Envie os arquivos ao repositório já existente com `git add -A`, `git commit` e `git push`.
2. Abra **Actions → Compilar APK Android** e acompanhe a execução.
3. Após sucesso, baixe o artifact **`MeuTreino-Android-v0.4.9-debug`**.
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

## Próxima atualização planejada: v0.5.0

Aprimorar progressão de cargas, controle do esforço e acompanhamento da evolução com base nos registros realizados. Antes disso, validar o catálogo e o planejador da v0.4.0 no Android e revisar exercícios com profissional habilitado.

### Mi Band 7 — diagnóstico BLE experimental (v0.3.0)

Em **Dispositivos → Conexões → Mi Band 7**, é possível procurar dispositivos BLE, testar a conexão GATT e verificar serviços padrão de bateria/FC. Isso **não é pareamento Huami nem acesso ao histórico**. Consulte `docs/MI_BAND_7_v0.3.0.md`.


### v0.4.1 — Demonstrações em vídeo (piloto)
Veja [docs/VIDEOS_EXERCICIOS_v0.4.1.md](docs/VIDEOS_EXERCICIOS_v0.4.1.md): player integrado com segmentos opcionais, cinco demonstrações inicialmente cadastradas e plano de curadoria para os 234 exercícios.


## Vídeos demonstrativos (v0.4.2)

Os vínculos exercício/vídeo são mantidos em `app/src/main/assets/exercise_videos.json` e validados em `scripts/validate_exercise_videos.py`. Veja `docs/ALTERACOES_v0.4.2.md`.

## Expansão dos vídeos demonstrativos (v0.4.3)

O catálogo contém 38 vínculos de vídeo para 234 exercícios. Veja `docs/VIDEOS_EXERCICIOS_v0.4.3.md` para fontes e status de verificação.

## Biblioteca de vídeos v0.4.4

44 dos 234 exercícios possuem vídeos individuais associados. Os outros 190 têm busca direcionada por exercício no YouTube, **não vídeos curados**. Veja `docs/ALTERACOES_v0.4.4.md`.

## Pesquisa de vídeos — v0.4.5

O `exercise_videos.json` mantém 44 demonstrações existentes em `videos`, 60 links identificados por metadados em `candidates` (abertura externa, sem auditoria visual nem cortes arbitrários) e 130 buscas em `discovery`. A lista e o estado de cada exercício estão em [`docs/LEVANTAMENTO_VIDEOS_v0.4.5.md`](docs/LEVANTAMENTO_VIDEOS_v0.4.5.md).

## Vídeos demonstrativos — v0.4.6

Após revisão manual do usuário no site, os 60 candidatos da v0.4.5 foram promovidos ao player do aplicativo: 45 vídeos integrais (até 40 segundos) e 15 trechos revisados de até 40 segundos. Há agora **104 vídeos no player**, nenhum candidato restante e **130 exercícios com busca específica**. Os 44 vídeos anteriores foram preservados. Consulte [docs/ALTERACOES_v0.4.6.md](docs/ALTERACOES_v0.4.6.md).
