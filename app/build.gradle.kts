plugins {
    id("tally.android.application")
    id("tally.android.compose")
    id("tally.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.tally"
    defaultConfig {
        applicationId = "com.tally"
        versionCode = 5
        versionName = "0.5.0"
        resourceConfigurations += setOf("en", "en-rGB")
    }
    // Room schema export — the migration test (ARCHITECTURE §6) reads these.
    sourceSets.named("androidTest") {
        assets.srcDir(layout.projectDirectory.dir("schemas"))
    }
}

// Export Room schemas to /schemas so migration tests can walk 1 -> current.
ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json) // type-safe nav routes

    // Room (entities + DAOs land in PR3; wiring KSP here so PR3 is purely additive).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager (used by the 22:00 reminder in M2).
    implementation(libs.androidx.work.runtime.ktx)

    // Crypto for smsRaw encryption + biometric.
    implementation(libs.google.tink.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // DataStore for app prefs (jewel accent choice, reminder time, etc.).
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // Paparazzi screenshot tests use JUnit4 rules; the Vintage engine lets them
    // run on the JUnit Platform alongside the JUnit5 unit tests. The Paparazzi
    // runtime itself is added by the plugin.
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.google.truth)
}

// Paparazzi goldens are recorded on the developer's OS (Windows). Cross-OS font
// rasterisation differs at the sub-pixel level, so a Linux CI run can diff
// against Windows goldens. Until goldens are re-recorded on Linux, CI excludes
// the screenshot tests by passing -PexcludeScreenshotTests; they still run (and
// must pass) on the developer's machine. See docs/DECISIONS/0004.
tasks.withType<Test>().configureEach {
    if (project.hasProperty("excludeScreenshotTests")) {
        exclude("**/*ScreenshotTest*")
    }
}
