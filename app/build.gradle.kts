plugins {
    id("tally.android.application")
    id("tally.android.compose")
    id("tally.android.hilt")
}

android {
    namespace = "com.tally"
    defaultConfig {
        applicationId = "com.tally"
        versionCode = 1
        versionName = "0.1.0-m1"
        resourceConfigurations += setOf("en", "en-rGB")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

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

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
}
