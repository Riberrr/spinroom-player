plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.riberrr.spinroom.core.audio"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Engine tests touch android.util.Log / Process; stubs return defaults instead of throwing.
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    api(project(":core:sendspin")) // public API uses StreamFormat, AudioChunk, PlayerState
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
