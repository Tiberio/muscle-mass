# Health Connect — diagnóstico e painel detalhado (v0.3.0)

## Problema identificado no teste em celular

A versão inicial só buscava dados ao tocar em **Atualizar dados** ou ao conceder permissões, e exibia poucas medidas sem suas datas. A frequência cardíaca era limitada às últimas 24 horas. Assim, uma consulta recente podia apresentar uma medição antiga como se fosse atual.

## Correções desta atualização

- Consulta automática **ao entrar na aba Dispositivos** e botão para atualização manual. Não há processo em segundo plano nem atualização em tempo real.
- Distinção entre **horário da consulta ao Health Connect** e **horário da medição** (peso, frequência cardíaca e sono).
- Passos por dia dos últimos 7 dias (agregação, sem somar fontes duplicadas).
- Última amostra de frequência cardíaca disponível nos últimos 7 dias, explicitamente marcada como histórica, não em tempo real.
- Últimas cinco sessões com duração, data e origem e contagem semanal.
- Último registro de sono, se a permissão opcional de sono for concedida e houver dados.
- Nenhuma integração adicional direta ou autenticada com a Mi Band 7 nesta etapa. O IAA continua independente.

## Diagnóstico para o usuário

1. Sincronize a Mi Band 7 **com o Mi Fitness** e confira a data da atividade no Mi Fitness.
2. No Health Connect, verifique **Dados e acesso / Gerenciar dados** e quais aplicativos registraram os dados relevantes. Confirme que o Mi Fitness tem permissão para **gravar** cada tipo de dado.
3. Abra **Meu Treino → Dispositivos → Conexões**, conceda apenas as permissões de leitura desejadas e toque em **Atualizar dados**.
4. Compare o **horário de medição** e a **origem** com os dados do Health Connect. A consulta no Meu Treino não obriga o Mi Fitness a transmitir dados novos.
5. A origem indicada (p. ex., Google Fit) identifica o aplicativo que gravou o dado no Health Connect, não prova o sensor físico de origem.

## Limitações

- As métricas exibidas dependem dos tipos de dados **efetivamente escritos** pelos aplicativos no Health Connect. Sono e sessões podem estar ausentes.
- O Health Connect não é um serviço de transmissão contínua da pulseira. A FC apresentada é uma **amostra histórica**.
- Dados novos de peso, passos e FC podem levar tempo para serem gravados pelo Mi Fitness ou aplicativo intermediário. O Meu Treino não controla esse intervalo.
- Para `StepsRecord`, usa-se `aggregate()` (recomendação do Android) para evitar duplicações na presença de várias fontes.
- Algumas leituras de histórico são limitadas por permissões e pelo recorte temporal do Android/Health Connect.
- O aplicativo não importa automaticamente os resultados para o IAA, evitando dupla contagem.

Referências oficiais: 
- https://developer.android.com/health-and-fitness/health-connect/read-data
- https://developer.android.com/health-and-fitness/health-connect/aggregate-data
- https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/SleepSessionRecord
