plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "be.tvgids.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "be.tvgids.app"
        minSdk = 26
        targetSdk = 34
        // In GitHub Actions telt elke build op, zodat je op het toestel ziet welke je hebt.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.0.$build"
    }

    // Vaste sleutel, zodat elke nieuwe build als update over de vorige
    // geïnstalleerd kan worden (anders moet je eerst de-installeren).
    signingConfigs {
        create("gedeeld") {
            storeFile = file("tvgids-debug.keystore")
            storePassword = "tvgids"
            keyAlias = "tvgids"
            keyPassword = "tvgids"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("gedeeld")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("gedeeld")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
