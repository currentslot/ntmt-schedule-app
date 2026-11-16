import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// Данные подписи не хранятся в репозитории: local.properties (ntmt.store.file,
// ntmt.store.password, ntmt.key.alias, ntmt.key.password) или переменные окружения
// NTMT_STORE_FILE, NTMT_STORE_PASSWORD, NTMT_KEY_ALIAS, NTMT_KEY_PASSWORD
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(prop: String, env: String): String? =
    localProps.getProperty(prop)?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("ntmt.store.file", "NTMT_STORE_FILE")
val releaseStorePassword = signingValue("ntmt.store.password", "NTMT_STORE_PASSWORD")
val releaseKeyAlias = signingValue("ntmt.key.alias", "NTMT_KEY_ALIAS") ?: "ntmt"
val releaseKeyPassword = signingValue("ntmt.key.password", "NTMT_KEY_PASSWORD") ?: releaseStorePassword
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null

android {
    namespace = "ntmt.schedule"
    compileSdk = 35
    defaultConfig {
        applicationId = "ntmt.schedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 136
        versionName = "3.0 Stable"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            // R8 убирает неиспользуемый код библиотек (в первую очередь material-icons-extended)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
    // Снимки экранов без телефона: ./gradlew testDebugUnitTest -Proborazzi.test.record=true
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.systemProperty("roborazzi.test.record", "true")
                it.maxHeapSize = "3g"
                // Образ Android для Robolectric можно скачать заранее и указать папку в ROBOLECTRIC_DEPS
                System.getenv("ROBOLECTRIC_DEPS")?.let { dir ->
                    it.systemProperty("robolectric.offline", "true")
                    it.systemProperty("robolectric.dependency.dir", dir)
                }
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    // Firebase тянет старый Fragment 1.1; для запроса разрешений нужен 1.3+
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.43.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.43.1")
}
