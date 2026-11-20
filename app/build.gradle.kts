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
    // Vaste sleutel voor de APK op GitHub, zodat elke versie als update over de vorige
    // installeert. Ook die staat enkel in de secrets en komt via APK_KEYSTORE binnen.
    val apkSleutel = System.getenv("APK_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }

    signingConfigs {
        if (apkSleutel != null) {
            create("apk") {
                storeFile = apkSleutel
                storePassword = System.getenv("APK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("APK_KEY_ALIAS")
                keyPassword = System.getenv("APK_KEYSTORE_PASSWORD")
            }
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

    // Debug-builds gebruiken de standaard debugsleutel van Android Studio.
    buildTypes {
        getByName("release") {
            // R8: kleinere app en moeilijker te ontleden code.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Uploadsleutel (Play Store), anders de APK-sleutel; lokaal blijft de release ongetekend.
            signingConfig = when {
                uploadSleutel != null -> signingConfigs.getByName("upload")
                apkSleutel != null -> signingConfigs.getByName("apk")
                else -> null
            }
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
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    testImplementation("junit:junit:4.13.2")
}
