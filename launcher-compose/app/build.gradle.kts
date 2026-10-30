plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.launcher.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.launcher.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"
    }

    signingConfigs {
        create("release") {
            // Anahtar yolu ve şifreler build.sh tarafından ortam değişkeni olarak verilir.
            storeFile = file(System.getenv("LAUNCHER_KEYSTORE") ?: "keystore.jks")
            storePassword = System.getenv("LAUNCHER_STORE_PASS") ?: ""
            keyAlias = "launcher"
            keyPassword = System.getenv("LAUNCHER_STORE_PASS") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        // Release lint analizi APK üretimine bağlı değil; bağımlılık indirme hatalarını önler.
        checkReleaseBuilds = false
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3:1.5.0-alpha29")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
}
