plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "be.tvgids.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "be.tvgids.app"
        minSdk = 26
        targetSdk = 36
        // In GitHub Actions telt elke build op, zodat je op het toestel ziet welke je hebt.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        // De Play Store weigert een versionCode die niet hoger is dan de vorige. Begint de
        // teller van GitHub ooit opnieuw (bv. na het hernoemen van de workflow), verhoog
        // dan deze basis tot boven de laatst geüploade versionCode.
        val versieBasis = 1000
        versionCode = versieBasis + build
        versionName = "1.0.$build"
    }

    // Uploadsleutel voor de Play Store. Die staat nooit in de repository: GitHub Actions
    // zet hem vanuit de secrets klaar en geeft het pad door via UPLOAD_KEYSTORE.
    val uploadSleutel = System.getenv("UPLOAD_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }

    signingConfigs {
        // Vaste testsleutel, zodat elke APK van GitHub als update over de vorige
        // geïnstalleerd kan worden. Niet gebruiken voor de Play Store.
        create("gedeeld") {
            storeFile = file("tvgids-debug.keystore")
            storePassword = "tvgids"
            keyAlias = "tvgids"
            keyPassword = "tvgids"
        }
        if (uploadSleutel != null) {
            create("upload") {
                storeFile = uploadSleutel
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("gedeeld")
        }
        getByName("release") {
            // R8: kleinere app en moeilijker te ontleden code.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Met uploadsleutel (Play Store) of anders de gedeelde testsleutel.
            signingConfig = signingConfigs.getByName(if (uploadSleutel != null) "upload" else "gedeeld")
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
        // Een release-build stopt bij ernstige problemen (lintVital).
        checkReleaseBuilds = true
        abortOnError = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
