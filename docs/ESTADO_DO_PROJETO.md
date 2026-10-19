# Estado de implementação — 17/10/2026

## Implementado em código

- Modelos de dados, catálogo local, planejador determinístico e análise exploratória de equilíbrio.
- DataStore para perfil e preferências, Room para sessões e séries.
- Wizard de configuração, revisão final completa, navegação e editor de preferências.
- Tela Hoje, proposta inicial, aba Plano (alteração de dia e exercício), histórico, dispositivos informativos e perfil.
- Registro persistente por série, encerramento de sessão e retomar treino incompleto.

## Comprovação feita

O código **puro Kotlin** em `domain/*.kt` compilou com `kotlinc 1.9.0` e executou 10 checagens sem falhas. O Android SDK e Gradle não estão disponíveis no ambiente de geração; logo a compilação das telas, o APK e a execução em dispositivo permanecem **não verificadas**.

## Fases futuras obrigatórias

- Validar compilação APK e navegação no celular.
- Fazer revisão de acessibilidade, layout e persistência após reinícios.
- Implementar permissões Health Connect e Bluetooth somente quando os adaptadores estiverem disponíveis.
- Trabalhar dados de saúde com deduplicação, procedência e autorização.
- Validar pessoalmente e/ou profissionalmente os exercícios e vídeos antes do uso definitivo.

### Ajustes finais nesta entrega
- A aba Hoje escolhe a próxima sessão ainda não concluída da semana, em vez de mostrar sempre a primeira.
- Trocas de exercícios/dias dentro do plano não invalidam o aceite da proposta; mudanças de perfil solicitam nova revisão.
- Cardio inicia em zero minutos para não registrar automaticamente atividade não realizada.
