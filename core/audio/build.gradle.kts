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
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.media3.common) // service package: Player state for the MediaSession
    api(libs.androidx.media3.session)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
