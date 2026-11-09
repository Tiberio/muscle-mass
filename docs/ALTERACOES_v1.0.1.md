# Meu Treino Android — v1.0.1 (correção da assinatura para atualizações)

**Base funcional:** v1.0.0, preservando o sistema de backup JSON, catálogo, treino, cardio, histórico, medidas e revisão inteligente. **Sem alteração de esquema Room**, que permanece na versão 5.

## Correções

- `versionName=1.0.1`, `versionCode=21`, artefato GitHub `MeuTreino-Android-v1.0.1-debug`.
- Metadado `appVersion` do JSON exportado atualizado para `1.0.1` (formato e compatibilidade mantidos).
- O GitHub Actions exige o secret `MEUTREINO_DEBUG_KEYSTORE_B64`, instala o keystore antes do Gradle e interrompe a compilação se a chave não estiver presente ou válida.
- Nova etapa obrigatória `scripts/verificar_assinatura_apk.sh`: compara a impressão digital SHA-256 do APK com a do certificado do keystore persistente. Não publica artefato se divergirem.
- Chave criada uma só vez via `scripts/criar_chave_debug_persistente.sh` no Debian, sem substituição automática.
- `.gitignore` protege contra inclusão acidental dos arquivos de keystore no repositório.
- Validador estático `scripts/validate_release_v101.py`, mantendo as verificações da v1.0.0.

## Instalação

O APK antigo v0.6.0/v1.0.0 pode estar assinado com outra chave; **a chave nova não atualiza essa instalação**. O usuário decidiu **desinstalar e instalar do zero**. Isso apaga os registros locais anteriores, inclusive fichas, séries e medidas. Não há necessidade de backup ADB se essa perda for conscientemente aceita.

Configure primeiro a chave e secret, compile a v1.0.1, veja o resultado `ASSINATURA OK`, **só então** desinstale a versão antiga e instale a nova. Da v1.0.1 em diante, preserve a mesma chave e aumente `versionCode` para atualizar por cima. Consulte `docs/ASSINATURA_PERSISTENTE_v1.0.1.md`.

## Testes

- Testes estáticos de vídeos, revisão inteligente, migrações SQL, backup e exigência de assinatura no workflow.
- Script de verificação SHA-256 testado em ambiente simulado com certificados correspondentes e diferentes.
- Criação da chave testada para garantir que execuções repetidas não a substituem.
- **Compilação Android completa, APK real e instalação no dispositivo ainda pendentes.**
