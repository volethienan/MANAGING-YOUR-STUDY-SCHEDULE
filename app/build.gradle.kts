import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.cuoiky_qllichhoctap"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.cuoiky_qllichhoctap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "OTP_BACKEND_URL", "\"${localProps.getProperty("OTP_BACKEND_URL", "http://10.0.2.2:8080")}\"")
        buildConfigField("String", "ADMIN_BACKEND_URL", "\"${localProps.getProperty("ADMIN_BACKEND_URL", "http://10.0.2.2:8090")}\"")
        buildConfigField("String", "FIREBASE_DATABASE_URL", "\"${localProps.getProperty("FIREBASE_DATABASE_URL", "https://mobile-263d4-default-rtdb.firebaseio.com")}\"")
    }

    buildFeatures {
        buildConfig = true
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
