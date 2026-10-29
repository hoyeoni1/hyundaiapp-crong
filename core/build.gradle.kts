plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hyundaiapp.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
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
    api("ai.pleos.playground:Vehicle:2.3.2")
    api("ai.pleos.playground:NaviHelper:3.0.5")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
