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
#   release    envia o main e dispara o workflow de release no GitHub, que
#              testa, builda, assina e publica a vX.Y.Z (do versionName) com o APK;
#              o app acha a versão nova sozinho
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
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
    release)
        # O APK sai do CI (.github/workflows/release.yml), assinado com a chave
        # de debug deste Mac guardada no secret DEBUG_KEYSTORE.
        cd "$SCRIPT_DIR"
        versao=$(grep -o 'versionName = "[^"]*"' app/build.gradle.kts | cut -d'"' -f2)
        tag="v$versao"
        if [ "$(git branch --show-current)" != "main" ]; then echo "A release sai do main."; exit 1; fi
        if [ -n "$(git status --porcelain)" ]; then echo "Há mudanças sem commit: a release tem que sair igual ao main."; exit 1; fi
        if git ls-remote --exit-code --tags origin "$tag" >/dev/null; then
            echo "A tag $tag já existe: suba versionName e versionCode em app/build.gradle.kts."
            exit 1
        fi
        git push origin main
        gh workflow run release.yml --ref main
        echo "Workflow disparado para $tag; acompanhando..."
        sleep 5
        id=$(gh run list --workflow release.yml --limit 1 --json databaseId --jq '.[0].databaseId')
        gh run watch "$id" --exit-status
        gh release view "$tag" --json url --jq .url
        ;;
    *) echo "Uso: ./dev.sh [emulator|test|build|install|start|run|logs|print|release]"; exit 1 ;;
esac
