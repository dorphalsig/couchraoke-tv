plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.couchraoke.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.couchraoke.tv"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Ktor
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    
    // QR Code
    implementation(libs.zxing.android.embedded)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // mDNS
    implementation(libs.jmdns)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
