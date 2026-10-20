# Alterações da v0.3.2

- Novo gráfico de passos semanais e gráfico de média diária das amostras de FC no painel de Dispositivos.
- Sono por dia (se autorizado), com fonte e data.
- IAA local vs IAA integrado experimental; datas e expiração preservadas.
- Leitura paginada de sessões e frequência cardíaca, com limite e aviso de consulta truncada.
- Modelo conservador para integração sem duplicar atividades já registradas.
- Conexões existentes (Health Connect, balança Xiaomi via BLE, Mi Band 7 via BLE/bateria) preservadas.
- Novo documento de fundamentos do IAA integrado e teste de regressão de cobertura, duplicação, janela móvel.
- Versão Android: `versionName=0.3.2`, `versionCode=5`; banco Room permanece versão 3.
- **Build Android não realizado localmente**: requer GitHub Actions. O APK só será disponibilizado após o workflow obter sucesso.

## Ajuste de apresentação — horário da leitura

- O Painel de saúde e atividade agora destaca **Última consulta ao Health Connect** logo no topo, com data e hora local (`dd/MM HH:mm`).
- Durante uma nova consulta, o indicador mostra o último horário **concluído**, com sinal visual de atualização em andamento. Se a leitura falhar, o horário não é substituído.
- O horário é registrado ao final da leitura bem-sucedida do Meu Treino, não no início da consulta.
- O painel também apresenta separadamente os horários da última amostra de frequência cardíaca e da última medição de peso.
- **Não é possível inferir esse horário como o da sincronização Zepp Life → Health Connect**. O envio pode ocorrer antes da leitura pelo Meu Treino.
- O ajuste não modifica IAA, banco de dados, permissões Bluetooth ou o esquema de versão (`0.3.2`).
