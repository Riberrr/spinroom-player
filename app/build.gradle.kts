import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Version comes from the release tag (v1.2.3), passed by CI as -PappVersion=1.2.3. Local builds use 0.0.0-dev.
val appVersion = (findProperty("appVersion") as String?)?.removePrefix("v") ?: "0.0.0-dev"
val appVersionCode = appVersion.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    .let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }
    .coerceAtLeast(1)

// Release signing: keystore.properties locally, or SIGNING_* env vars in CI. Without either, the debug key is used.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String, env: String): String? = keystoreProps.getProperty(key) ?: System.getenv(env)

android {
    namespace = "io.github.riberrr.spinroom"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.riberrr.spinroom"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    val storePath = signingValue("storeFile", "SIGNING_STORE_FILE")
    val releaseSigning = if (storePath != null) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(storePath)
            storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
        }
    } else {
        null
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Until the real key is in place, fall back to the debug key so the APK still installs on the TV.
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:design"))
    implementation(project(":core:sendspin"))
    implementation(project(":core:audio"))
    implementation(project(":core:ma-api"))
    implementation(project(":core:data"))
    implementation(project(":feature:nowplaying"))
    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:details"))
    implementation(project(":feature:sources"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
