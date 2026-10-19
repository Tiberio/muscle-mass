# Meu Treino v0.2.0 — alterações e limitações

Base: **v0.1.1 com a correção `clearMessage()` já aplicada**. Esta é uma atualização funcional, não uma renumeração do pequeno reparo de compilação.

## Correções dos testes no celular

1. Cursor e inserção nos campos do cadastro: estado local `TextFieldValue`, independente da atualização assíncrona do DataStore. Peso admite decimais.
2. Campo de limitações: mesma estratégia de edição local.
3. Histórico: botão **Excluir esta sessão**, com confirmação, removendo séries dependentes; preserva a ficha de treino e recalcula indicadores.
4. Aba **Hoje**: só libera a sessão programada para a data local atual, exibindo como futura a próxima sessão; respeita dias de descanso. Treinos perdidos ficam disponíveis para reorganização na aba Plano (reagendamento mais inteligente ainda pendente).
5. Mudança do ambiente padrão no Perfil: o planejador recebe o novo ambiente; substituições salvas incompatíveis são ignoradas; mudanças no perfil pedem nova aceitação do plano.
6. Botão **Alterar ambiente somente hoje** abre caixa de seleção; não altera a preferência salva.
7. Divisão de treinamento com >=4 sessões: alternância superior/inferior e rotação de candidatos para evitar fichas A/B idênticas. Isso **não é controle médico de recuperação**, ainda depende de confirmação e observação real.

## Novos recursos

- Nova escolha **Frequência do cardio**: nos dias de musculação, 3, 5 ou 7 dias.
- Planejador de cardio diário independente dos dias de musculação; nos dias sem força, sugere intensidade leve/moderada quando apropriado.
- Registro de cardio feito fora da musculação: Histórico → Saúde.
- Registro da intensidade do cardio na finalização do treino de força.
- **IAA (Índice de Atividade Acumulada)** com pontos recebidos hoje, pontos expirados hoje e histórico por cada um dos sete últimos dias.
- Métrica diferencia cardio moderado/vigoroso, cardio leve e bônus diário de força. A meta 100 é uma **convenção**, não marcador médico.
- Documentação de fontes oficiais e limites: `docs/FUNDAMENTACAO_CIENTIFICA.md`.
- Migração explícita do banco Room v1 → v2: histórico anterior preservado **se a instalação puder ser atualizada sem desinstalar**.

## Limitações importantes

- Não foi possível compilar as telas Android completas neste ambiente; apenas os módulos Kotlin puros foram testados. Execute o GitHub Actions.
- A v0.2.0 **ainda não lê dados de Mi Band 7 / Zepp Life / balança / Health Connect**. Os pontos vêm de sessões e cardio cadastrados no próprio app.
- O IAA não é PAI, não prediz risco e não é um índice clinicamente validado.
- A programação da recuperação ainda não calcula a carga real de todos os músculos e a intensidade do treino a partir de RIR histórico. Há heurísticas e avisos, mas não garantia de 48 horas efetivas em todos os agendamentos.
- A escolha “todos os dias (7)” é **nova**: usuários que vêm de v0.1.1 devem abrir Perfil → Frequência do cardio e marcar essa opção.
- Na v0.1.1, o cardio não tinha classificação de intensidade. Sessões antigas permanecem no Histórico, mas não geram pontos aeróbicos retroativos por suposição.
- Dados importados de diferentes fontes ainda exigirão deduplicação por atividade, não apenas por ID; essa integração não está pronta.
- A contagem é por **dias locais**, inclusive hoje, e se atualiza ao reabrir/atualizar a interface. A atualização automática precisamente à meia-noite está prevista para etapa futura.

## Instalação por GitHub Actions e assinatura

O GitHub Actions de `assembleDebug` gera, em cada runner temporário, uma chave de depuração que pode mudar entre execuções. Um novo APK pode falhar ao atualizar a instalação antiga com `INSTALL_FAILED_UPDATE_INCOMPATIBLE` mesmo que o `applicationId` seja o mesmo. **Não desinstale sem aceitar o possível apagamento dos dados locais.** Para preservar dados em upgrades futuros, configure uma chave de assinatura estável via Secrets, sem publicar a chave privada no repositório. A versão v0.1.1 já instalada talvez use uma chave efêmera indisponível.

Se você deseja preservar os dados de teste antigos, não desinstale imediatamente: antes precisamos verificar a possibilidade de backup por ADB (`run-as`, por se tratar de um build debuggable). Caso sejam apenas sessões de teste descartáveis, poderá desinstalar a v0.1.1 e instalar a v0.2.0 quando sua compilação for validada.

## Testes Kotlin executados

- 14 verificações do núcleo de planejamento e equilíbrio.
- 8 verificações de distribuição do cardio em 5+7 dias.
- 8 verificações da contagem diária e expiração de pontos.
- Total: **30 verificações isoladas**, sem substituir compilação Android e testes no aparelho.
