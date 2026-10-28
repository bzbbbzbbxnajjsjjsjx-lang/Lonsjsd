plugins {
    id("com.android.test")
}

android {
    namespace = "com.example.minilauncher.benchmark"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    // Measures the app built with the "benchmark" build type, which is a release-like build.
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test:runner:1.6.2")
}
