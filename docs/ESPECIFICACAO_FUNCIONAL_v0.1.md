# Aplicativo Android de Treinamento — Especificação Funcional v0.1

**Estado:** definição inicial de produto; não representa prescrição de treino nem compatibilidade técnica comprovada com aparelhos.
**Identidade visual aprovada:** *Moderno Escuro* — fundo azul-marinho, cartões escuros, azul vivo como destaque; interface clara, limpa, sem publicidade, botões grandes durante treinos.

## 1. Visão do produto

Aplicativo Android pessoal de musculação e atividades aeróbicas. O usuário informa objetivos, preferências, disponibilidade e equipamentos; o aplicativo propõe treinos balanceados, explica decisões e acompanha sua execução e evolução. O algoritmo principal de seleção será determinístico, auditável e adaptável ao histórico.

**Princípios:** controle do usuário; explicabilidade; simplicidade; privacidade local; autonomia sem assinaturas/conta/servidor na primeira versão; interoperabilidade de dados de saúde quando possível; não presumir compatibilidade com fabricantes.

## 2. Primeira utilização: oito etapas

### 1 — Boas-vindas
- Apresentação curta das funcionalidades, privacidade e fluxo; botão **Começar**.

### 2 — Perfil Físico
- Idade (necessária para algumas personalizações), altura (opcional), peso (opcional), experiência em musculação, prática regular/retorno, limitações de movimento (pode optar por não informar); sexo para estimativas fisiológicas (opcional).
- Campo **Peso atual** aceita digitação ou ação **Importar da balança**; disponibilidade de importação depende de integração efetivamente funcional.
- Não exigir informação de diagnóstico médico, nem permitir que prescrição automatizada substitua orientação médica em caso de restrições importantes.

### 3 — Objetivos e Prioridades Musculares
- Objetivo principal entre saúde/bem-estar, hipertrofia, força, condicionamento e redução de gordura; até dois secundários.
- Regiões prioritárias opcionais: peito, costas, ombros, braços, abdômen, pernas, glúteos; seleção de até três regiões (limite preliminar da interface, revisável).
- Futuramente incorporar mapa anatômico frontal/posterior interativo.
- Destacar objetivamente que regiões não prioritárias também receberão estímulo.
- **Configuração usada como exemplo nas conversas:** objetivo saúde e bem-estar; sem secundários; prioridades peito, braços, ombros. Não tratá-la como plano final até revisão.

### 4 — Local e Equipamentos
- Selecionar múltiplos ambientes: casa sem equipamentos, casa equipada, academia.
- **Salvar persistentemente um ambiente principal/padrão**; usá-lo para gerar o plano ordinário.
- Permitir **troca somente para a sessão atual**, sem modificar o padrão; substituir exercícios e reavaliar equilíbrio de movimentos.
- Para casa equipada, catálogo de halteres, banco, barra fixa, elásticos, barra e anilhas, kettlebell, paralelas, colchonete; cardio: esteira, bicicleta, elíptico, corda. Ampliável.
- Modelo de equipamento por ambiente; peso disponível e detalhes finos configuráveis mais tarde.

### 5 — Disponibilidade e Rotina
- Rotina com dias definidos ou frequência semanal flexível.
- Duração padrão por sessão: 20, 30, 45, 60 minutos ou **Outro tempo** (entrada numérica em minutos, validada e salva).
- Opção de horário preferido (manhã, tarde, noite, sem preferência); duração variável dia a dia.
- Cálculo de tempo semanal e ajustes de sessão sem alterar duração padrão; exercícios, aquecimento e descanso precisam caber no tempo total.
- Reorganização de treinos não realizados sem compensação automática excessiva.

### 6 — Atividades Aeróbicas e Cardio
- Modalidades: caminhada, corrida, bicicleta, elíptico, corda, outra; ou não incluir cardio por enquanto.
- Momento: após musculação, dias separados ou misto.
- Duração: 10, 15, 20, 30 minutos ou outro tempo editável; intensidade leve, moderada, vigorosa ou adaptativa.
- Combinar cardio com o limite de tempo total, quando na mesma sessão.
- Troca de atividade para o dia sem sobrescrever a preferência padrão.

### 7 — Dispositivos (nome final da aba; substitui “Integração Xiaomi”)
- Primeiro identificar dispositivos existentes e aplicativos usados; usuário atualmente possui **Mi Band 7** e **Mi Body Composition Scale 2** e usa **Zepp Life**.
- Métodos distintos: (a) **Buscar por Bluetooth** compatível, (b) **Escolher marca/modelo** em catálogo com status explícito, (c) **Conectar aplicativo de saúde** via Health Connect quando os dados estiverem disponíveis.
- Catálogo inicial a investigar: Xiaomi, Samsung, Garmin, Polar, Amazfit, Google/Fitbit, Wahoo, Withings e balanças BLE selecionadas. Listar modelos como ‘compatível/testado’, ‘via aplicativo’ ou ‘não verificado’; não alegar compatibilidade universal.
- Saúde: frequência cardíaca, passos/sessões, peso/composição corporal, sono, calorias e outras medições somente se efetivamente publicadas/autorizadas.
- Importação opcional de histórico, se viável. Não presumir que o Zepp Life fornece API oficial aberta nem que PAI original esteja disponível pelo Health Connect.
- Prioridade futura: testar integração da Mi Body Composition Scale 2; investigar referências de código aberto como openScale, respeitando licenças e segurança.
- Nenhum dispositivo ou dado deve ser mostrado como sincronizado sem confirmação real.

### 8 — Revisão Final (exceção intencional ao padrão de telas curtas)
- **Uma única tela longa, rolável**, com todos os dados fornecidos nas etapas 1–7 organizados por seções.
- Cada seção/pergunta possui **Editar**, que abre diretamente a pergunta original. **Salvar e voltar à revisão** retorna diretamente à etapa 8, sem revisitar outras perguntas, preservando idealmente a posição de rolagem.
- Ao final da página: **Confirmar e gerar meu plano**.
- A proposta de treino é apresentada **após** a confirmação, em tela distinta, permitindo aceitar, modificar exercícios ou solicitar revisão; só depois o plano entra no uso cotidiano.

**Regra global de onboarding:** exceto a etapa 8, perguntas agrupadas em telas curtas com uma pergunta principal, botões Voltar/Próximo fixos, barra de progresso e indicação por **frações inteiras** (1/3, 2/3, 3/3; jamais “5.1/5”).

## 3. Navegação principal após aceitar o plano

**Barra inferior de cinco abas:**
1. **Hoje** — treino do dia, tempo estimado, ambiente atual, progresso semanal, início rápido, alternativa de treino/atividade.
2. **Plano** — agenda de sessões, detalhes, mudanças de local/duração para uma sessão, regeneração controlada.
3. **Histórico** — séries, repetições, cargas, cardio, tendências e medidas corporais, distinguindo dado manual e importado.
4. **Dispositivos** — catálogo, Health Connect, Bluetooth, permissões, última sincronização, qualidade/origem dos dados.
5. **Perfil** — metas, preferências, ambientes (incluindo padrão), equipamentos, duração, cardio e limitações.

**Execução do Treino** não ocupa uma aba fixa: é aberta por **Começar treino** e usa interface focada com vídeos, séries/repetições/cargas, esforço (RIR/opção simples), descanso e substituição de exercícios.

## 4. Fluxos essenciais

### Entrada e aceite
Abrir app → [sem cadastro?] Boas-vindas → Perfil Físico → Objetivos → Local → Rotina → Cardio → Dispositivos → Revisão rolável → [Editar pergunta → Salvar → volta direta à Revisão] → Confirmar → Gerar proposta → [Aceitar / Ajustar] → Hoje.

### Treino padrão vs. exceção
Hoje → Exibir sessão baseada no **ambiente padrão salvo** → Alterar ambiente/tempo **só hoje** (opcional) → adaptar exercícios → Começar → registrar → Concluir → Histórico. Alterar o ambiente temporariamente **não** sobrescreve o padrão.

### Registro e dispositivos
App do fabricante/Health Connect ou BLE compatível → solicitações de permissão explícitas → Health Data Gateway → validação/origem/deduplicação → banco local → histórico e métricas. Ausência de sincronização não deve impedir treino manual.

## 5. Motor de treino (fase após protótipo)
- Entradas: objetivo, experiência, volume atual, disponibilidade, ambiente/equipamentos, grupos prioritários, padrões de movimento já cobertos, histórico e recuperação.
- Exercícios caracterizados por região muscular primária/secundária, padrão (push/pull/squat/hinge/lunge/core), equipamento, dificuldade, equivalentes, vídeo curado.
- Propostas explicadas ao usuário, com equilíbrio entre regiões prioritárias e grupos complementares.
- Progressão por histórico de carga/reps/RIR e progressões de calistenia; cardio compatível com duração da sessão.
- Alteração de local/equipamento não deve esconder ausência de exercícios equivalentes para puxadas ou outros movimentos.
- Regras de segurança e limites de confiança claros; não transformar indicadores de frequência cardíaca em diagnóstico.

## 6. Dados / integrações
- Primeira versão: Kotlin, Jetpack Compose, Room/SQLite, DataStore e arquitetura modular tipo MVVM; sem login e sem backend.
- Módulos futuros: Health Connect e BLE/GATT quando compatível.
- Manter uma estrutura `source` para cada medição (ex.: Health Connect, Bluetooth, entrada manual, app de fabricante se disponível), timestamps, IDs estáveis e deduplicação.
- Índice próprio de atividade semanal (conceito similar à motivação do PAI, mas fórmula independente, documentada e validada futuramente). PAI Xiaomi mostrado somente se realmente acessível.
- Vídeos do YouTube escolhidos previamente por exercício: abrir app/player oficial inicialmente; incorporar player se viável e em conformidade com regras do serviço.

## 7. Marcos de implementação (propostos)
1. **Protótipo navegável Android** com dados simulados, onboarding e retorno à revisão, cinco abas, tela de treino demonstrativa.
2. **Persistência local** de perfil, ambientes, rotinas, plano e histórico.
3. **Catálogo curado de exercícios e vídeos** com seleção/substituições.
4. **Motor determinístico de planos**, explicação e progressão.
5. **Health Connect**, permissões, leitura e proveniência de dados.
6. **Bluetooth BLE** padronizado e modelos específicos verificados (incluindo teste da balança Xiaomi).
7. **Indicador de atividade**, gráficos, histórico/importação e refinamento visual.

## 8. Critérios de aceite do primeiro protótipo
- Fluxo de onboarding utilizável e curto, exceto revisão final rolável.
- Frações inteiras no progresso de subtelas.
- Escolha ‘Outro tempo’ salva corretamente; total de tempo calculado.
- Suporte a múltiplos ambientes; alteração da sessão **não** modifica ambiente padrão.
- Editar na revisão retorna diretamente à revisão e apresenta a informação atualizada.
- Confirmação conduz à apresentação de um plano simulado (identificado como tal).
- Cinco abas acessíveis; execução do treino fica fora da barra inferior.
- Dispositivos demonstrativos não são apresentados como efetivamente conectados.
- Tudo funciona localmente com dados simulados, inclusive sem rede.

## 9. Pendências de decisão / investigação
- Nome final do aplicativo, ícone e idioma além de português.
- Escolha de um esquema de horários/calendário quando ocorrer variação por dia.
- Definir exemplos de vídeos curados e licenciamento/termos de reprodução.
- Determinar quais métricas específicas chegam ao Health Connect a partir dos aparelhos e apps do usuário.
- Confirmar viabilidade e protocolo BLE específico da Mi Body Composition Scale 2 e tratamento de múltiplos apps.
- Definir regras quantitativas de volume, frequência e pontuação semanal antes do motor real.
