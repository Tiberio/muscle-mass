#!/usr/bin/env bash
# Cria apenas uma chave de depuração persistente. Nunca compartilhe o arquivo.
set -euo pipefail
umask 077
command -v keytool >/dev/null || { echo 'ERRO: instale openjdk-17-jdk (Debian) para usar keytool.' >&2; exit 1; }
DIR="${HOME}/.local/share/meutreino-assinatura"
KEY="${DIR}/debug.keystore"
mkdir -p "$DIR"
chmod 700 "$DIR"
if [[ -e "$KEY" ]]; then
  echo "Chave já existente: $KEY (não será substituída)."
else
  keytool -genkeypair -storetype JKS -keystore "$KEY" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 3072 \
    -validity 10000 -dname 'CN=Meu Treino debug, OU=Uso pessoal, O=Meu Treino, C=BR' -noprompt
fi
chmod 600 "$KEY"
# Falhar caso haja arquivo antigo corrompido ou credenciais diferentes.
keytool -list -keystore "$KEY" -storepass android -alias androiddebugkey >/dev/null
keytool -list -v -keystore "$KEY" -storepass android -alias androiddebugkey | grep -E 'SHA256:|Alias name:|Valid from:' || true
cat <<EOF

CHAVE DE DESENVOLVIMENTO CRIADA EM: $KEY
IMPORTANTE: esta chave é DIFERENTE da utilizada no APK v0.6.0 já instalado.
Ela NÃO permite instalar por cima da v0.6.0. Somente uma reinstalação limpa resolve o conflito inicial.

GitHub -> repositório -> Settings -> Secrets and variables -> Actions
-> New repository secret
Nome do secret: MEUTREINO_DEBUG_KEYSTORE_B64
Valor: saída deste comando no seu terminal (NÃO publique em chats nem arquivos git):

base64 -w0 "$KEY"; echo

Guarde o arquivo $KEY em local privado e mantenha um backup offline.
Esta chave é somente para APK de desenvolvimento, não para publicar na Play Store.
Veja docs/ASSINATURA_PERSISTENTE_v1.0.1.md para a ordem segura de instalação.
EOF
