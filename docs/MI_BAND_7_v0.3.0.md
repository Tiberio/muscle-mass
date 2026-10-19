# Mi Band 7 no Meu Treino — diagnóstico experimental da v0.3.0

## Resultado e limitações

A Mi Band 7 comum (não Pro) usa o ecossistema Huami/Zepp OS e é suportada amplamente pelo projeto livre Gadgetbridge. **Isso não significa que o Meu Treino consiga ler diretamente dados privados da pulseira:** seu protocolo exige **autenticação Huami** e não fornece uma API pública universal de sincronização de histórico.

Esta atualização oferece **acesso experimental ao Bluetooth LE**, executado *somente quando o usuário o solicita*, para:

1. procurar anúncios BLE próximos por até 20 segundos;
2. listar resultados com nome, identificador parcial e RSSI (sinal); o nome não comprova identidade;
3. conectar temporariamente a um dispositivo escolhido e descobrir serviços GATT;
4. ler o nível de bateria através do serviço padrão `0x180F`, **se exposto e autorizado**;
5. ouvir amostras de frequência cardíaca do serviço padrão `0x180D` / característica `0x2A37`, **se exposto e notificações forem permitidas**.

Os dados da Mi Band 7 (passos, sono, treino, PAI, histórico de FC) normalmente usam serviços proprietários e autenticação específica: **não estão implementados** neste diagnóstico. O sistema não informa falsamente conexão completa. Dados BLE lidos não alimentam o IAA nesta etapa. Nenhuma chave ou endereço Bluetooth é salvo.

## Teste no celular

1. Deixe o Bluetooth ativo e a Mi Band 7 próxima do celular.
2. Abra **Dispositivos → Conexões → Mi Band 7 — Bluetooth experimental**.
3. Toque **Procurar Mi Band 7**, conceda permissão de *Dispositivos próximos* e aguarde.
4. Procure `Mi Band 7`, `Xiaomi Smart Band 7` ou outro nome plausível. Um dispositivo sem nome pode aparecer: a identificação é inconclusiva; **não escolha dispositivos de terceiros**.
5. Toque **Testar** e observe a conexão, os serviços, a eventual bateria e a frequência cardíaca.
6. Toque **Desconectar** quando terminar.

Se a pulseira não aparecer ou o GATT falhar, o Zepp Life pode estar ocupando a conexão ou a pulseira pode estar anunciando sem nome/fora do alcance. **Não desvincule nem redefina a pulseira** para realizar este teste. Fechar temporariamente o app de sincronização é uma tentativa reversível, mas não garante sucesso.

No Android 12+, Bluetooth requer `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT`. Em Android 11 ou anterior, há exigência de permissão de localização para varredura BLE. O manifesto declara `neverForLocation`: certos anúncios podem ser filtrados pelo sistema.

## Caminho de dados sem autenticação adicional

Se o usuário preferir manter Zepp Life, verificar no aparelho se há **Zepp Life → Google Fit** e **Google Fit → Health Connect**. Os dados só aparecerão em Meu Treino se essas sincronizações estiverem disponíveis, operando e autorizadas. No painel **Health Connect**, a origem de FC informa o aplicativo intermediário, **não a identidade comprovada da pulseira**. Não importar automaticamente para o IAA sem proteção contra duplicatas.

## Para uma futura integração de histórico

Projetos de referência:

- [Gadgetbridge — suporte Xiaomi/Mi Band 7](https://gadgetbridge.org/gadgets/wearables/xiaomi/)
- [Gadgetbridge — autenticação Huami](https://gadgetbridge.org/basics/pairing/huami-xiaomi-server/)
- [Gadgetbridge — recursos Zepp OS](https://gadgetbridge.org/basics/topics/zeppos/)
- [Android — permissões Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Android — BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback)

As abordagens completas possíveis incluem: (a) um mecanismo de autenticação de dispositivo Huami no Meu Treino; (b) migração voluntária do usuário para Gadgetbridge, com exportação de dados mediante consentimento; (c) integração indireta via Health Connect. Cada alternativa possui compromissos e necessita testes reais. Não introduzir dependência da conta Xiaomi nem recolher credenciais da conta no aplicativo sem revisão de segurança adequada.
