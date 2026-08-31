plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.signagetv.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.signagetv.tv"
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "1.3.0"

        // ------------------------------------------------------------------
        //  Configuracion de fabrica de la app.
        //  Se compila dentro del APK para que una TV recien instalada arranque
        //  sola, sin que nadie tenga que escribir nada con el control remoto.
        //  Si cambias la cuenta o el servidor, tocalo aca y recompila el APK.
        // ------------------------------------------------------------------
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://149.50.138.58\"")
        buildConfigField("String", "DEFAULT_USERNAME", "\"Ninos\"")
        buildConfigField("String", "DEFAULT_PASSWORD", "\"Jorge123\"")
        // true = al instalar, entra sola y empieza a reproducir en modo
        // "Automatico segun horario" (todo se maneja desde el panel web).
        buildConfigField("boolean", "AUTO_START", "true")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
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
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.security.crypto)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    implementation(libs.coil)
}
