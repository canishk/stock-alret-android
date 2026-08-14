import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun signingProperty(name: String): String? =
    localProperties.getProperty(name) ?: System.getenv(name)

val releaseStoreFile = signingProperty("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
val hasReleaseKeystore = releaseStoreFile != null && releaseStoreFile.exists()

android {
    namespace = "com.stockpricealert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stockpricealert"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        buildConfigField(
            "String",
            "RAPIDAPI_KEY",
            "\"${localProperties.getProperty("RAPIDAPI_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "RAPIDAPI_HOST",
            "\"indian-stock-exchange-api2.p.rapidapi.com\""
        )
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingProperty("RELEASE_STORE_PASSWORD")
                keyAlias = signingProperty("RELEASE_KEY_ALIAS")
                keyPassword = signingProperty("RELEASE_KEY_PASSWORD")
                    ?: signingProperty("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
