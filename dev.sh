#!/usr/bin/env bash
# Atalhos de build/deploy (Mac). Uso: ./dev.sh <comando>
#
#   emulator   abre o emulador em background
#   test       testes do motor
#   build      APK release (R8), o que vai para o celular
#   install    instala o release no dispositivo conectado
#   start      abre o app
#   run        build + install + start (o mais usado)
#   logs       logcat do processo do app
#   print X    salva um print da tela em prints/X.png
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD_NOME="${AVD_NOME:-trombadario_test}"
PACOTE="com.frentefria"
APK="$SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"

# O launcher abre pelo alias ligado (Icone.kt): tenta o do clima e, se estiver desligado, o direto.
abrir() {
    "$ADB" shell am start -n "$PACOTE/.IconeClima" 2>&1 | grep -q Error && "$ADB" shell am start -n "$PACOTE/.IconeDireto" >/dev/null
    true
}

case "${1:-run}" in
    emulator) "$EMULATOR" -avd "$AVD_NOME" >/dev/null 2>&1 & disown ;;
    test) (cd "$SCRIPT_DIR" && ./gradlew :motor:test) ;;
    build) (cd "$SCRIPT_DIR" && ./gradlew assembleRelease) ;;
    install) "$ADB" install -r "$APK" ;;
    start) abrir ;;
    run)
        (cd "$SCRIPT_DIR" && ./gradlew assembleRelease)
        "$ADB" install -r "$APK"
        abrir
        ;;
    logs) "$ADB" logcat --pid="$("$ADB" shell pidof -s "$PACOTE")" ;;
    print)
        mkdir -p "$SCRIPT_DIR/prints"
        "$ADB" exec-out screencap -p > "$SCRIPT_DIR/prints/${2:-print}.png"
        echo "prints/${2:-print}.png"
        ;;
    *) echo "Uso: ./dev.sh [emulator|test|build|install|start|run|logs|print]"; exit 1 ;;
esac
