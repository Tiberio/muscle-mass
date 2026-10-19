#!/usr/bin/env bash
# Compilação LOCAL, sem baixar ferramentas ou modificar o Debian.
# Para compilar remotamente sem instalar versões modernas no Debian,
# execute o GitHub Actions em .github/workflows/android-debug.yml.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo 'O Android SDK não está configurado (ANDROID_HOME/ANDROID_SDK_ROOT).'
  exit 1
fi
if [[ -f gradle/wrapper/gradle-wrapper.jar && -f gradlew ]]; then
  echo 'O wrapper do projeto pode baixar o Gradle. Para manter instalação exclusivamente pelo Synaptic, utilize o GitHub Actions.'
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Gradle não encontrado. Não farei downloads automáticos.'
  exit 1
fi
version=$(gradle --version | sed -n 's/^Gradle \([0-9][^ ]*\)$/\1/p' | head -1)
if [[ "$version" != "9.4.1" ]]; then
  echo "Gradle instalado: ${version:-desconhecido}. O projeto requer Gradle 9.4.1."
  echo 'Não alterarei o Debian. Use o fluxo remoto documentado em COMPILAR_COM_GITHUB.md.'
  exit 1
fi
exec gradle --no-daemon :app:assembleDebug
