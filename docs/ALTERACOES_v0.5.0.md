# Meu Treino Android — v0.5.0

**Base:** v0.4.10. `versionCode = 18`. Publicação do código-fonte: versão proposta para validação via GitHub Actions e em dispositivo Android.

## 1. Ajuste solicitado dos vídeos

Na tela **Plano → Explorar biblioteca → Ver orientações**, o exercício com vídeo aprovado apresenta somente **Abrir vídeo no Youtube**. As opções principais duplicadas e o botão **Testar player incorporado (experimental)** foram removidos. O player WebView deixou de ser chamado pela interface; a ação abre a página oficial do vídeo no YouTube, preservando o ponto inicial sugerido. O YouTube pode continuar além do fim do trecho selecionado. O exercício sem vídeo aprovado usa o mesmo botão para abrir uma busca claramente identificada como não verificada. A seção **Quer mais opções de vídeos?** continua disponível para links externos ainda não revisados.

## 2. Evolução do desempenho

**Histórico → Evolução** inclui agrupamento por ID estável de exercício, total de séries em sessões concluídas, última carga/repetições/RIR, gráfico da maior carga registrada em cada sessão e lista recente de séries. Exercícios sem carga continuam listados, mas não recebem valores inventados. O número de exposições por grupo muscular considera apenas sessões concluídas com pelo menos uma série realmente registrada para o grupo.

## 3. Avaliação corporal

**Histórico → Medidas** inclui cadastro manual de circunferência em cm para 11 locais padronizados, valor decimal com vírgula/ponto, histórico datado por local, gráfico, diferença entre primeira e última medição e exclusão individual com confirmação. Dados ficam no Room local; não são inferidos de balança ou Health Connect. Não apresentam diagnóstico médico.

## 4. Histórico de versões da ficha

**Histórico → Fichas** consulta snapshots imutáveis do plano, com criação de versão ao aprovar/alterar a ficha ou iniciar sessões em um ambiente diferente. Cada sessão nova recebe `planVersionId`, possibilitando contagens reais de sessões por versão e exposições por grupo muscular. O conteúdo da ficha armazenado inclui o ambiente, a duração de musculação, dias e exercícios planejados. O ID/fingerprint SHA-256 é derivado do conteúdo para evitar versões repetidas quando nada mudou.

Sessões anteriores à atualização permanecem intactas, com `planVersionId = NULL`, identificadas na UI como registros legados sem vínculo retroativo. A migração não tenta inventar fichas históricas para sessões antigas.

## 5. Persistência e migração

Room sobe de **v3 para v4** por `MIGRATION_3_4`, que:

- acrescenta a coluna anulável `sessions.planVersionId`;
- cria `body_measurements` e índice por local/data;
- cria `plan_versions` com fingerprint exclusivo;
- preserva as tabelas `sets`, `sessions`, `weight_measurements`, `cardio_activities`.

Não há migração destrutiva nem mudança no DataStore, nas configurações de treinos, no cardio independente ou na integração com Xiaomi/Health Connect.

## 6. Testes e limitações

- `python3 scripts/validate_exercise_videos.py` verifica os 233 vídeos principais e a única ação externa de vídeo.
- `python3 tests/RoomMigrationV4SmokeTest.py` simula as operações SQL de migração sobre dados antigos.
- Testes JUnit para validação de medidas e fingerprints dos planos.
- **Pendente:** compilação e testes instrumentados no Android real, incluindo atualização sem desinstalar, gravação de medidas, início/conclusão de treino e histórico das fichas.
- A versão 0.5.0 **não implementa** recomendação automática de troca de ficha, análise de recuperação nem gatilho dos 80%; reservados à v0.6.0 conforme especificação v2.

## 7. Roteiro de aceitação no celular

1. Instale **por cima** da v0.4.10, sem apagar os dados. Confira Histórico → Sessões e peso anterior.
2. Plano → Explorar biblioteca → Ver orientações → teste o botão único **Abrir vídeo no Youtube**.
3. Histórico → Medidas: registre e confira duas medidas do mesmo local, gráfico e exclusão de uma medição de teste.
4. Histórico → Evolução: visualize exercício com sessões anteriores concluídas; não invente carga para exercícios com peso corporal.
5. Histórico → Fichas: confira versão inicial; inicie e conclua sessão nova e observe o contador de execução real.
6. Verifique História, Dispositivos, Hoje e o gerenciamento normal dos ambientes.


### Atenção: assinatura de APK de depuração

Os APKs de testes anteriores foram gerados em executores efêmeros do GitHub Actions. Uma assinatura `debug` diferente pode fazer o Android **recusar uma atualização sem desinstalar**. Como `android:allowBackup="false"`, desinstalar pode apagar os dados locais. Se aparecer **"App não instalado"**, **"assinaturas incompatíveis"** ou mensagem similar, **não desinstale nem limpe o armazenamento**. Antes, providencie cópia segura das informações com ferramentas apropriadas (por exemplo, acesso ADB a aplicativos `debuggable`, conforme compatibilidade do aparelho), ou configure uma chave de assinatura persistente para versões futuras.

A migração Room protege dados em instalações atualizadas normalmente, mas não pode recuperar dados apagados por uma desinstalação.
