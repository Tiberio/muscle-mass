# Meu Treino — integração de dispositivos (v0.3.0)

## Objetivo

A v0.3.0 implementa leitura **real e opt-in** de registros existentes no Health Connect e um leitor experimental da **Mi Body Composition Scale 2**. Nenhum resultado é fabricado para simular conexão. O código do planejador e o índice IAA da v0.2.0 permanecem sem alteração.

## 1. Health Connect

Dependência `androidx.health.connect:connect-client:1.1.0` (estável). Compatível com dispositivos Android 9+ com serviços Google Play e provedor Health Connect (Android 14+ no sistema; Android 9–13 requer app separado). A disponibilidade pode variar com o aparelho e o perfil do Android.

Acesso somente **leitura**, solicitado pelo usuário na aba **Dispositivos → Conexões**:

- passos hoje e nos últimos sete dias (agregação que evita somar incorretamente registros sobrepostos);
- pesagem mais recente dos últimos 30 dias;
- amostra mais recente de frequência cardíaca das últimas 24 horas;
- total de sessões registradas nos últimos sete dias (até 1.000 registros lidos).

O aplicativo informa o pacote de origem do último peso e da frequência cardíaca. Os passos agregados podem vir de várias fontes: **não são automaticamente dados da Mi Band 7**. Só é possível visualizar dados de dispositivos cujos aplicativos realmente tenham gravado no Health Connect e para os quais o usuário tenha concedido acesso. O Zepp Life e o Mi Band 7 **não recebem integração direta nesta versão**.

### Privacidade

- Não há leitura em segundo plano, upload a servidor, conta de usuário ou telemetria.
- O aplicativo não solicita gravação no Health Connect.
- A autorização pode ser revogada nas configurações do Android.
- Dados consultados via Health Connect ficam apenas na memória enquanto o processo Android estiver ativo. Sua revogação impede novas consultas.
- Tela `PermissionsRationaleActivity` fornece justificativa acessível ao gerenciador de permissões.

## 2. Mi Body Composition Scale 2 via Bluetooth Low Energy

A balança transmite o peso por pacotes Bluetooth LE sem necessariamente precisar parear. Nosso leitor procura **Service Data** do UUID `0x181B` e exige:

1. pacote de 13 bytes, formato documentado experimentalmente;
2. flag de pesagem estabilizada e flag de balança carregada;
3. unidade quilograma e intervalo plausível;
4. confirmação manual antes de salvar a medição.

A busca dura até 25 segundos e pode ser cancelada. Permissões só são solicitadas ao tocar em **Procurar pesagem**. No Android 12+ são `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT`; no Android 11 ou anterior, o Android exige permissão de localização para procurar dispositivos BLE, embora o aplicativo não processe localização. Dependendo da versão do Android, também pode ser necessário ativar os serviços de localização para o scanner funcionar.

O procedimento é **experimental e não testado no hardware específico do usuário**. Modelos/firmwares diferentes podem variar o anúncio BLE ou impedir a recepção simultânea com outros aplicativos.

O código não importa o histórico armazenado internamente na balança, não realiza pareamento autenticado nem calcula percentuais de gordura, massa muscular, 'idade corporal', gordura visceral ou impedância.

### Proteção de privacidade

Como a transmissão pode ser recebida de uma balança usada por outra pessoa próxima, a tela mostra o peso encontrado, e a gravação exige o comando explícito **Confirmar e salvar meu peso**. As pesagens salvas ficam em tabela Room local `weight_measurements`. A aba **Perfil** pode importar a pesagem confirmada mais recente. Não se armazena o endereço Bluetooth do aparelho.

## 3. Índice IAA

O IAA **não importa automaticamente** passos, calorias, sessões ou frequência cardíaca do Health Connect nesta versão. Isso evita dupla contagem com os exercícios manuais. A definição de uma regra de deduplicação por identificadores, tempo, origem e intensidade será feita após validar dados reais e consistência entre fontes.

## 4. Migração sem perda de dados

Banco Room `version=3`. Migração `2 → 3` **aditiva**, cria `weight_measurements`; migração `1 → 2` continua registrada. Dados de sessões, séries, cardio e configurações existentes devem ser preservados **quando a assinatura de atualização do APK for a mesma**. Uma compilação diferente do GitHub Actions pode possuir assinatura de depuração diferente; veja a orientação de backup no README.

## 5. Testes manuais (celular)

1. Atualizar app mantendo dados; conferir histórico de treinos e IAA.
2. Em **Dispositivos → Conexões**, tocar em **Autorizar leitura** e selecionar permissões desejadas no Android; as permissões são independentes; o painel mostra os tipos autorizados.
3. Tocar em **Atualizar dados**; avaliar números e verificar se a fonte mostrada corresponde ao app que disponibilizou os registros.
4. Negar permissão e testar atualização: aplicativo deve mostrar mensagem e continuar funcionando.
5. Com Bluetooth ativo, tocar em **Procurar pesagem**, subir na Mi Body Composition Scale 2 e esperar a estabilização.
6. Confirmar um peso e verificar o registro em **Histórico → Saúde**.
7. Em **Perfil → Perfil físico**, tocar em **Importar da balança**, caso exista pesagem confirmada.
8. Conferir que os valores da musculação/cardio e do IAA não foram modificados pela simples leitura de Health Connect.

## 6. Limites, referências técnicas

- Google: [Health Connect — começar](https://developer.android.com/health-and-fitness/health-connect/get-started)
- Google: [Health Connect — disponibilidade](https://developer.android.com/health-and-fitness/health-connect/availability)
- Google: [Leitura e agregação de dados](https://developer.android.com/health-and-fitness/health-connect/read-data)
- AndroidX: [Health Connect 1.1.0](https://developer.android.com/jetpack/androidx/releases/health-connect)
- Projeto de engenharia reversa: [ble-in-xiaomi / huami.health.scale2.md](https://github.com/freenetwork/ble-in-xiaomi/blob/master/doc/devices/huami.health.scale2.md)
- Projeto livre: [openScale — Xiaomi Bluetooth Mi Scale](https://github.com/oliexdev/openScale/wiki/Xiaomi-Bluetooth-Mi-Scale)

> Dados de saúde fornecidos por aplicativos e sensores não têm garantia de precisão clínica. O IAA é uma métrica própria e não reproduz o PAI Xiaomi.
