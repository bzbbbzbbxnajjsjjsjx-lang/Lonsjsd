#!/usr/bin/env bash
# Compose launcher'ı imzalı release APK olarak derler.
# Gereksinim: ANDROID_HOME (platforms;android-36, build-tools;36.0.0), JDK 17+, gradle 8.x.
# Çıktı: $OUT_DIR/launcher-release.apk (varsayılan: launcher-compose/build)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="${OUT_DIR:-$HERE/build}"
export LAUNCHER_KEYSTORE="${LAUNCHER_KEYSTORE:-$HOME/.config/launcher/launcher.keystore}"
export LAUNCHER_STORE_PASS="${LAUNCHER_STORE_PASS:-android}"

cd "$HERE"
"${GRADLE_BIN:-gradle}" --no-daemon :app:assembleRelease

mkdir -p "$OUT_DIR"
cp app/build/outputs/apk/release/app-release.apk "$OUT_DIR/launcher-release.apk"
"$ANDROID_HOME/build-tools/37.0.0/apksigner" verify --verbose "$OUT_DIR/launcher-release.apk" | tail -3
echo "APK hazır: $OUT_DIR/launcher-release.apk"
