plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.gamevault.univibe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gamevault.univibe"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.glide)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

dependencies {
    // 1. Importa la plataforma BoM de Firebase (administra las versiones automáticamente)
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))

    // 2. Dependencia para Cloud Storage (para subir la imagen)
    implementation("com.google.firebase:firebase-storage")

    // 3. Dependencia para Realtime Database (para guardar el enlace de la imagen)
    implementation("com.google.firebase:firebase-database")

    // 4. Dependencia para Authentication (para identificar qué usuario subió la imagen)
    implementation("com.google.firebase:firebase-auth")
}
