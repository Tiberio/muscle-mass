#!/usr/bin/env python3
"""Valida versao e cadeia de assinatura da v1.0.1. Nao substitui compilacao Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = (root/'app/build.gradle.kts').read_text()
workflow = (root/'.github/workflows/android-debug.yml').read_text()
ignored = (root/'.gitignore').read_text()
verify = (root/'scripts/verificar_assinatura_apk.sh').read_text()
create = (root/'scripts/criar_chave_debug_persistente.sh').read_text()

assert 'versionName = "1.0.1"' in app
assert 'versionCode = 21' in app
assert '"appVersion" to "1.0.1"' in (root/'app/src/main/java/br/com/meutreino/data/LocalBackup.kt').read_text()
assert 'MeuTreino-Android-v1.0.1-debug' in workflow
assert 'MEUTREINO_DEBUG_KEYSTORE_B64' in workflow
assert 'secrets.MEUTREINO_DEBUG_KEYSTORE_B64' in workflow
assert 'base64 --decode > "$HOME/.android/debug.keystore"' in workflow
assert 'test -s "$HOME/.android/debug.keystore"' in workflow
assert 'keytool -list -keystore "$HOME/.android/debug.keystore"' in workflow
assert 'bash scripts/verificar_assinatura_apk.sh' in workflow
assert 'ASSINATURA OK' in verify
assert 'EXPECTED' in verify and 'ACTUAL' in verify
assert 'if [[ "$EXPECTED" != "$ACTUAL" ]]' in verify
assert 'if [[ -e "$KEY" ]]' in create, 'Script nao pode sobrescrever chave existente'
assert '*.keystore' in ignored and '*.jks' in ignored
print('RELEASE v1.0.1 OK: versionCode 21, versionName 1.0.1, secret obrigatorio, comparacao SHA-256 e chave protegida')
