// App del reloj (Wear OS). Comparte el applicationId con la app del celular:
// así Android las reconoce como "la misma app" y las deja hablar entre ellas.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.utom.buenvivir.reloj"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.utom.buenvivir"
        minSdk = 30          // Wear OS 3 en adelante (Galaxy Watch4)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)   // Box, Column, textos y botones
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // Hablar con el celular
    implementation(libs.play.services.wearable)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
