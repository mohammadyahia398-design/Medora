plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // TEMPORARY id: must be finalized before the first Google Play release
    namespace = "com.example.medora"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.medora"
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
        }
        release {
            isMinifyEnabled = false
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}
