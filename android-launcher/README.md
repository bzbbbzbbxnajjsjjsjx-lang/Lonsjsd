# Mini Launcher

Sadece uygulama isimlerini gösteren, Material 3 Expressive ile yapılmış sade bir Android launcher.

## Özellikler

- Yüklü ve başlatılabilir tüm uygulamalar isimlerine göre sıralı listelenir.
- Arama kutusuna yazdıkça liste anında filtrelenir; isim öneki eşleşenler üstte gelir.
- Klavyedeki "Ara" tuşu ilk sonucu açar.
- Uygulama yüklenince, silinince veya güncellenince liste kendiliğinden yenilenir.
- Ana ekrana her dönüşte arama kutusu temizlenir.
- Android 12+ cihazlarda duvar kağıdından dinamik renk (Material You) kullanılır.

## Derleme

1. Klasörü Android Studio ile açın (`android-launcher/`). Gradle wrapper'ı Android Studio kendisi oluşturur.
2. Sync edin, ardından `app` modülünü bir cihaza veya emülatöre çalıştırın.
3. Ana ekran olarak seçmek için Ayarlar → Ana ekran (Home) → Mini Launcher'ı seçin.

Uygulama paketi adı `com.example.minilauncher` olarak ayarlıdır; kendi paket adınızı kullanmak için `app/build.gradle.kts` içindeki `namespace` ve `applicationId` değerlerini değiştirin.

## Sürümler

- Kotlin 2.0.21, AGP 8.7.3, compileSdk / targetSdk 36, minSdk 26
- Compose BOM 2025.10.01 (Material 3 1.4.x, `MaterialExpressiveTheme` ve `MotionScheme.expressive()`)
