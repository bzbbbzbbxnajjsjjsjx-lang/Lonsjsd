#!/usr/bin/env bash
# Launcher web arayüzünü içeren imzalı APK'yı üretir.
# Gereksinim: ANDROID_HOME (build-tools 35.0.0 ve platforms;android-35), JDK 17+, keytool.
# Çıktı: $OUT_DIR/launcher.apk (varsayılan: launcher-android/build)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
WEB="$HERE/../launcher"
OUT_DIR="${OUT_DIR:-$HERE/build}"
SDK="${ANDROID_HOME:?ANDROID_HOME tanımlı değil}"
BT="$SDK/build-tools/35.0.0"
PLATFORM_JAR="$SDK/platforms/android-35/android.jar"
# Keystore çıktı klasörünün dışında tutulur; aynı anahtarla imzalanan sürümler üst üste kurulabilir.
KEYSTORE="${KEYSTORE:-$HOME/.config/launcher/launcher.keystore}"
mkdir -p "$(dirname "$KEYSTORE")"
KS_PASS="android"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"/{assets,gen,classes,dex,compiled}

# 1) Web dosyalarını assets klasörüne kopyala (sadece uygulamanın kullandığı dosyalar).
cp "$WEB/index.html" "$WEB/manifest.webmanifest" "$WEB/icon.svg" "$OUT_DIR/assets/"

# 2) Kaynakları derle ve APK iskeletini bağla.
"$BT/aapt2" compile --dir "$HERE/res" -o "$OUT_DIR/compiled/res.zip"
"$BT/aapt2" link -o "$OUT_DIR/unsigned.apk" \
    -I "$PLATFORM_JAR" \
    --manifest "$HERE/AndroidManifest.xml" \
    -A "$OUT_DIR/assets" \
    --java "$OUT_DIR/gen" \
    --min-sdk-version 24 --target-sdk-version 35 \
    "$OUT_DIR/compiled/res.zip"

# 3) Java kodunu derle ve dex'e çevir.
javac -source 8 -target 8 -Xlint:-options -bootclasspath "$PLATFORM_JAR" \
    -d "$OUT_DIR/classes" \
    "$HERE/src/com/launcher/app/MainActivity.java" \
    $(find "$OUT_DIR/gen" -name '*.java')
"$BT/d8" --release --min-api 24 --lib "$PLATFORM_JAR" \
    --output "$OUT_DIR/dex" $(find "$OUT_DIR/classes" -name '*.class')
(cd "$OUT_DIR/dex" && zip -q -j "$OUT_DIR/unsigned.apk" classes.dex)

# 4) Hizala ve imzala.
"$BT/zipalign" -f 4 "$OUT_DIR/unsigned.apk" "$OUT_DIR/aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias launcher -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Launcher, O=Personal, C=TR"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --out "$OUT_DIR/launcher.apk" "$OUT_DIR/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT_DIR/launcher.apk"
echo "APK hazır: $OUT_DIR/launcher.apk"
