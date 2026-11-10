#!/usr/bin/env bash
# Verifica se o APK debug foi assinado com a chave persistente, nao com a chave aleatoria do runner.
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
KEY="${MEUTREINO_DEBUG_KEYSTORE_PATH:-${HOME}/.android/debug.keystore}"
APKSIGNER="${ANDROID_HOME:?ANDROID_HOME nao definido}/build-tools/36.0.0/apksigner"

for file in "$APK" "$KEY" "$APKSIGNER"; do
  if [[ ! -f "$file" ]]; then
    echo "ERRO: arquivo necessario ausente: $file" >&2
    exit 1
  fi
done

EXPECTED="$(keytool -list -v -keystore "$KEY" -storepass android -alias androiddebugkey 2>/dev/null | awk '/SHA256:/ { gsub(":", "", $2); print tolower($2); exit }')"
ACTUAL="$("$APKSIGNER" verify --print-certs "$APK" | awk -F ': ' '/Signer #1 certificate SHA-256 digest:/ { print tolower($2); exit }')"

if [[ ! "$EXPECTED" =~ ^[0-9a-f]{64}$ || ! "$ACTUAL" =~ ^[0-9a-f]{64}$ ]]; then
  echo 'ERRO: nao foi possivel determinar as impressoes digitais SHA-256.' >&2
  exit 1
fi

if [[ "$EXPECTED" != "$ACTUAL" ]]; then
  echo 'ERRO: o APK nao foi assinado com a chave persistente configurada!' >&2
  echo "Esperado: $EXPECTED" >&2
  echo "Encontrado: $ACTUAL" >&2
  exit 1
fi

echo "ASSINATURA OK: certificado APK coincide com a chave persistente. SHA-256: $ACTUAL"
