# Fundamentação científica e decisões de projeto — Meu Treino

**Versão da documentação: 0.2.0 (19/10/2026)**

Este documento registra quais orientações publicadas usamos no aplicativo, quais decisões são **regras de engenharia** (não prescrições universais) e quais recursos ainda requerem validação. Destina-se a revisão pública, transparência e manutenção do projeto. Não substitui avaliação médica, fisioterapêutica nem orientação individual por profissional de Educação Física.

## 1. Atividade aeróbica (OMS)

Para adultos, a Organização Mundial da Saúde recomenda **150–300 min/semana** de atividade aeróbica **moderada** ou **75–150 min/semana** de atividade **vigorosa**, ou combinação equivalente, além de exercícios de fortalecimento de todos os grandes grupos musculares **em pelo menos dois dias por semana**. A orientação é **começar com pouco e progredir gradualmente**. Essas são metas gerais, não obrigação de treinar musculação em apenas dois dias nem proibição de cardio diário.

**Regra do aplicativo:** perguntar disponibilidade, modalidades e frequência escolhidas; tentar preservar cardio diário quando solicitado. Variar intensidade e volume para evitar dias consecutivos excessivamente exigentes, especialmente quando combinados com musculação. Mostrar minutos moderados equivalentes em sete dias como métrica separada da pontuação.

Fontes: OMS, *WHO Guidelines on Physical Activity and Sedentary Behaviour*, 2020 — https://www.ncbi.nlm.nih.gov/books/NBK566046/ ; https://www.who.int/health-topics/noncommunicable-diseases/physical-activity

## 2. Força, adaptação e regularidade (ACSM, 2026)

O posicionamento ACSM de 2026 destaca regularidade, adequação ao objetivo, treino dos grandes grupos musculares **ao menos duas vezes por semana**, progressão e a viabilidade de exercícios com pesos livres, máquinas, elásticos e peso corporal. Não existe divisão universal obrigatória A/B/C; repetir movimentos pode favorecer aprendizagem e avaliação de progressão.

**Regra do aplicativo:** não gerar A e B como simples reordenação da mesma ficha. Distribuir *padrões de movimento*, músculos principais e secundários, volume de séries e dias de exposição. Priorizar as preferências do usuário, sem excluir grupos musculares importantes. O modelo continua conservador e incompleto sem avaliação profissional e catálogo de exercícios amplo.

Fonte: ACSM, *Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews*, anúncio 18/03/2026 — https://acsm.org/science-spotlight-acsm-releases-new-position-stand-on-resistance-training/ ; resumo gráfico: https://www.acsm.org/wp-content/uploads/2026/03/Resistance-Training-Position-Stand-infographic.pdf

## 3. Descanso e recuperação

Recuperar um grupo muscular após sessões intensas importa. A referência prática **~48 h entre sessões de maior intensidade do mesmo grupo** aparece em orientações da American Heart Association (AHA), mas **não é uma lei universal**: depende de carga, volume, experiência, sono, percepção de esforço, dor e recuperação individual. Treino consecutivo pode funcionar alternando grupos musculares. Cardio leve/moderado também pode ocorrer nos dias sem musculação; o algoritmo deve evitar assumir que todo cardio exige descanso completo.

**Regra do aplicativo:** verificar para cada músculo principal e secundário quando houve último estímulo relevante; preferir alternância superior/inferior para cinco dias consecutivos. A versão 0.2.0 usa heurísticas de distribuição e avisos, **não mede recuperação fisiológica real**. Dias de força e dias de cardio são tratados separadamente.

Fonte: AHA, *Strength and Resistance Training Exercise*, revisado em 19/01/2024 — https://www.heart.org/en/healthy-living/exercise-and-physical-activity/fitness-basics/strength-and-resistance-training-exercise

## 4. Intensidade e 'teste da fala' (CDC)

Intensidade **moderada**: geralmente é possível conversar, mas não cantar. **Vigorosa**: é difícil dizer muitas palavras seguidas sem pausar para respirar. São métodos aproximados, dependentes da condição individual.

**Regra do aplicativo:** na versão inicial, o usuário classifica a sessão como leve, moderada ou vigorosa. Não deduzimos intensidade de passos, apenas da modalidade ou de frequência cardíaca sem aferição. Se intensidade for desconhecida, não inventamos pontos de cardio. Futuras medições contínuas de FC requerem calibração, permissão e tratamento de dados ausentes.

Fonte: CDC, *How to Measure Physical Activity Intensity*, 04/12/2025 — https://www.cdc.gov/physical-activity-basics/measuring/index.html

## 5. Gordura abdominal

Exercícios aeróbicos estão associados a melhorias no peso, cintura e tecido adiposo visceral. A metanálise de 116 ensaios randomizados (6.880 pessoas) mostra associação dose–resposta até cerca de 300 min semanais; isso **não é promessa de emagrecimento localizado** nem substitui alimentação e avaliação de composição corporal.

**Regra do aplicativo:** cardio não será excluído automaticamente porque existe musculação; as metas devem ser ajustadas à disponibilidade e recuperação. Não sugerir que abdominais eliminem gordura abdominal de forma localizada.

Fonte: Jayedi et al., *JAMA Network Open*, 26/12/2024, DOI: 10.1001/jamanetworkopen.2024.52185 — https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2828487

## 6. PAI versus índice próprio IAA

O PAI é um sistema de pontuação *proprietário/validado em seu contexto de pesquisa*, centrado em frequência cardíaca e acumulado numa janela móvel de sete dias. Não temos o algoritmo completo do PAI nem acesso assegurado ao PAI registrado no Zepp Life. Não podemos chamar nossa pontuação de PAI, tratá-la como equivalente nem transpor suas associações epidemiológicas.

A partir da versão 0.2.0, implementamos o **IAA — Índice de Atividade Acumulada**, **heurístico e não validado clinicamente**, com sete **dias de calendário** incluindo hoje (não janela exata das últimas 168 horas):

- Cada minuto de **cardio moderado** recebe **2/3 ponto**; **vigoroso** recebe **4/3 pontos**. A equivalência 2:1 reflete as recomendações de minutos aeróbicos, **não uma equação fisiológica de risco**.
- Cardio **leve** ou de intensidade **desconhecida** fica registrado, mas recebe **0 pontos aeróbicos** nesta implementação conservadora (isso **não** significa falta de benefício à saúde).
- Cada dia com sessão **concluída de musculação** que tenha ao menos uma série registrada recebe **4 pontos suplementares**, **no máximo uma vez por dia**. Esse bônus é uma **opção de design**, não um valor de referência científica.
- Saldo no dia D = soma dos pontos ganhos entre **D−6 e D**. Ao virar o dia, pontos obtidos em D−7 deixam de compor o saldo; não se subtraem pontos arbitrariamente.
- A pontuação pode superar 100; **100 não é um limiar clínico validado**. Separadamente, o aplicativo mostra *minutos moderados equivalentes* e *dias com musculação*, evitando afirmar cumprimento de diretrizes só com o IAA.
- O aplicativo mostra ganhos de hoje e pontos expirados hoje, para explicar variação diária do saldo.
- Eventos são identificados por ID; dados sincronizados no futuro deverão ser deduplicados por origem e identificador. Importações reais de Xiaomi/Health Connect ainda não estão implementadas.
- Histórico anterior de cardio cuja **intensidade não foi informada** não ganha pontos retroativos por suposição.

Referência conceitual para a **janela móvel**, não para nossa fórmula: NTNU/CERG, *Personal Activity Intelligence* — https://www.ntnu.edu/cerg/personal-activity-intelligence

### Exemplo

30 minutos moderados na segunda geram 20 pontos. Se não houver mais atividade, esses pontos permanecem de segunda a domingo e expiram na segunda seguinte. Um dia com cardio moderado de 30 min + musculação concluída registra 24 pontos. Não existe “penalidade por descansar”; simplesmente os dias antigos saem da janela.

## 7. Segurança e transparência

O app **não prescreve tratamento**: restrições, dor persistente, sintomas de alarme, condição clínica e dúvidas sobre contraindicações exigem profissional habilitado. As estimativas de recuperação e de intensidade são simplificações. O app deve sempre mostrar a justificativa de alterações e preservar treinos anteriores quando o planejamento é modificado. **Nunca converter dado ausente em 0 minuto de atividade ou em diagnóstico**.

## 8. Pendências científicas e técnicas

- Validar a heurística IAA com profissionais e usuários antes de qualquer alegação de saúde; não alegar benefício de mortalidade associado ao IAA.
- Medir sobreposição e recuperação em séries efetivas por músculo, com histórico e esforço real (hoje principalmente regras de agenda e aviso).
- Melhorar catálogo e classificação anatômica, vídeos e alternativas.
- Testar integração real Xiaomi / Zepp Life / balança / Health Connect; não prometer PAI original acessível.
- Considerar idioma, fuso horário, datas de viagem e sincronização duplicada na próxima evolução.
