import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    // TEMPORARY id: must be finalized before the first Google Play release
    namespace = "com.example.medora"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.medora.studyapp"
        minSdk = 26
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
    }

    // Fixed DEV key (public on purpose): lets new builds update the installed app.
    // NOT for Google Play. The real release key must stay secret.
    signingConfigs {
        create("dev") {
            storeFile = file("medora-dev.p12")
            storeType = "PKCS12"
            storePassword = "medora-dev"
            keyAlias = "medoradev"
            keyPassword = "medora-dev"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
            // R8 on the test APK too: smaller and faster app, and release-like shrinking.
            // Names are not obfuscated, so crash reports stay readable.
            // Turn off with medora.r8Debug=false in gradle.properties if a build or crash needs it.
            val r8Debug = (project.findProperty("medora.r8Debug") as String?)?.toBoolean() ?: true
            isMinifyEnabled = r8Debug
            isShrinkResources = r8Debug
            if (r8Debug) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                    "proguard-debug.pro",
                )
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("io.github.kyant0:backdrop:1.0.6")
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // On-device OCR for photos and scanned PDF pages (bundled Latin model, offline).
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
