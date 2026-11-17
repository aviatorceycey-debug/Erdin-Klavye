plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.erdin.klavye"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.erdin.klavye"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.customview:customview:1.2.0")
    testImplementation("junit:junit:4.13.2")
}
