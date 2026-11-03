import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val backendProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun backendString(key: String): String = "\"" + backendProperties.getProperty(key, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\""
// 제출 release는 시연 데이터 모드가 기본(PRD 7절, 11-03 장호연 결정). 실제 데이터로 빌드하려면 local.properties에 supabase.demo=false
fun backendFlag(key: String, default: Boolean): Boolean = when (val v = backendProperties.getProperty(key)?.trim()) {
    null, "" -> default
    "true" -> true
    "false" -> false
    else -> throw GradleException("local.properties $key must be true or false, was '$v'")
}

android {
    namespace = "com.hyundaiapp.cruisebreaker"
    compileSdk = 35
    useLibrary("android.car", false)

    defaultConfig {
        applicationId = "com.hyundaiapp.cruisebreaker"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        buildConfigField("String", "SUPABASE_URL", backendString("supabase.url"))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", backendString("supabase.publishableKey"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        getByName("debug") { buildConfigField("boolean", "SUPABASE_SIMULATED", "true") }
        getByName("release") { buildConfigField("boolean", "SUPABASE_SIMULATED", backendFlag("supabase.demo", true).toString()) }
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
    implementation(project(":core"))
    // RecordingService의 ServiceCompat.startForeground(4인자)는 1.12+. 다른 라이브러리를 통해 간접으로만 들어오지 않게 직접 둔다(이슈 #11)
    implementation("androidx.core:core-ktx:1.13.1")
    // 안내 음성(RiskSpeaker). 기기 내(ON_DEVICE) 모드라 서버 등록 키가 필요 없다
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")

    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
