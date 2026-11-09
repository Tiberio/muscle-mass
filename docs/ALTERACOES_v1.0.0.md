# Meu Treino Android 1.0.0 — Consolidação da primeira versão estável (candidata)

**Data:** 09/11/2026 · **Base validada pelo usuário:** v0.6.0 (compilação, aviso de revisão, cancelamento, dupla confirmação, aplicação da troca e histórico #1/#2).
**Código da versão:** `versionName=1.0.0`, `versionCode=20`. **Room continua na versão 5.**

## Objetivo desta atualização

Consolidar o fluxo já testado das versões 0.4.x–0.6.0, sem reescrever a biblioteca, o planejador, o cardio, o IAA ou a revisão inteligente. A principal novidade é uma estratégia explícita de **exportação/restauração local de dados**, necessária para cópias pessoais e eventual migração de dispositivo.

## Funcionalidades novas

1. **Perfil → Cópia de segurança → Exportar backup (.json).** Usa o seletor de documentos do Android (Storage Access Framework). Inclui preferências e os **nove conjuntos de dados** do Room: sessões, séries, cardio, pesagens, circunferências, versões de fichas, recuperação, metas corporais e decisões de revisão. Não exige permissão de acesso a todo o armazenamento.
2. **Perfil → Restaurar backup (.json).** Confirmação antes de abrir e validar o arquivo. O sistema verifica identificador de formato, versão de esquema, SHA-256, dados numéricos, unicidade de IDs e vínculos entre registros. O banco é gravado numa transação Room.
3. **Configuração inicial → Boas-vindas → Restaurar backup (.json).** Permite recuperar dados em uma instalação nova **antes** de preencher as perguntas do onboarding.
4. **Proteção dos dados existentes:** a restauração é **recusada se houver registros reais** (treinos, séries, cardio, medidas, pesagens, metas, recuperação ou decisões) no celular. Uma ficha inicial gerada durante o onboarding, sem registros, pode ser substituída pela ficha do backup. Não há importação/mesclagem destrutiva de registros.
5. **Backup portátil:** JSON legível, formato `br.com.meutreino.backup`, esquema 1. SHA-256 verifica integridade; **não constitui criptografia**. Limite de 30 MB por arquivo nesta primeira versão.
6. **Mais validações no GitHub Actions:** checagem de nove coleções, rota de backup na interface, existência da proteção contra sobrescrita e integridade estrutural das alterações. As validações já existentes de catálogo, revisão e migrações são preservadas.

## O que foi preservado

- Biblioteca com 234 exercícios; 233 links principais de vídeo e 172 alternativas ainda não revisadas; acesso externo ao YouTube, sem WebView experimental.
- Histórico → Sessões, Evolução, Medidas, Fichas e Saúde.
- Alternância de treinos, restrições de equipamento/ambiente, duração independente de musculação e cardio.
- Xiaomi/Health Connect, com as limitações e permissões já documentadas; IAA e seus gráficos.
- Motor multifatorial de revisão de fichas da v0.6.0, limiar antecipado de 80%, cancelamento, confirmações e histórico de versões.

## Privacidade e limites relevantes

- **O arquivo exportado contém informações pessoais e medidas corporais em texto claro.** Guarde-o em um local privado e, se usar nuvem pessoal, cuide das permissões. Não compartilhe o JSON como anexo público em repositórios Git.
- **Não** inclui históricos armazenados apenas no Health Connect, chaves de autenticação Xiaomi, permissões Android nem dados Bluetooth experimentais. Somente registros realmente salvos no banco próprio do aplicativo entram na cópia.
- Não há sincronização automática, backup na nuvem, rotina agendada, importação a partir de arquivos antigos de outros formatos, mesclagem entre duas instalações nem restauração em banco com registros reais.
- A verificação SHA-256 detecta inconsistências acidentais; não garante autenticidade contra alguém que tenha alterado arquivo e digest em conjunto.
- A restauração escreve primeiro os dados Room e depois as preferências DataStore. Não existe transação distribuída entre esses dois armazenamentos; uma falha abrupta entre eles poderá exigir conferência manual. **Não apagar a instalação de origem antes de conferir o resultado.**
- `android:allowBackup=false` permanece ativo: o sistema Android não faz uma cópia automática do banco. A exportação manual é indispensável.
- O workflow continua gerando **APK de depuração**, não APK/AAB assinado para publicação pública. O número 1.0.0 identifica a primeira candidata estável para uso pessoal, condicionada aos testes no dispositivo.

## Testes locais da entrega

- `scripts/validate_exercise_videos.py`: catálogo de 234 e UI do YouTube mantidos.
- `scripts/validate_review_feature.py`: interface de revisão, confirmações e motor mantidos.
- `tests/RoomMigrationV4SmokeTest.py` e `tests/RoomMigrationV5SmokeTest.py`: banco e migrações anteriores mantidos.
- `scripts/check_kotlin_imports.py`: sem importações Kotlin duplicadas.
- `scripts/validate_release_v100.py`: cobertura estrutural de exportação e importação, ordem de sessões/séries e fluxo de interface.
- `LocalBackup.kt`: compilação isolada pelo compilador Kotlin com stubs de Room e JSON, para detecção de erros sintáticos. **Isso não equivale ao build Android completo.**

## Testes indispensáveis no celular antes de marcar 1.0.0 como estável

1. Atualize **sem desinstalar** a v0.6.0. Confira se as fichas #1 e #2, sessões, medidas e vídeos continuam disponíveis.
2. Em **Perfil → Cópia de segurança**, exporte um arquivo JSON. Verifique se o Android confirmou o salvamento e se o arquivo não está vazio. Guarde-o em lugar privado.
3. Na instalação que já contém seu histórico, tente **Restaurar** este mesmo arquivo: o aplicativo deve recusar a operação sem apagar ou duplicar registros. O arquivo permanece intacto.
4. Para testar restauração real, use **um aparelho/perfil Android de teste sem dados** (ou um ambiente de teste separado), não desinstale o app que contém seu histórico. Na tela de boas-vindas, selecione o arquivo. Compare a contagem de sessões, fichas, medidas e perfil com a origem.
5. Feche e reabra o aplicativo de teste, confirme que os dados persistem e que não houve duplicação.
6. Verifique lançamento de sessão, vídeo do YouTube, cardio e revisão inteligente após atualizar. **Nunca remova a instalação de origem apenas para testar a importação.**

## Próximos passos para distribuição ampla

Se os testes de aceitação forem bem-sucedidos: produzir uma assinatura Android persistente, decidir como administrar a chave privada em um ambiente seguro, preparar AAB de release e política de privacidade, e validar comportamento com usuários/profissionais. Isso é distinto do APK de depuração disponível agora.
