package com.tally.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Applies Android application + Kotlin + ktlint + detekt + JUnit5 with Tally's
 * defaults: API floor 26, target 34, Java 17 toolchain, single dark theme,
 * Material 3 components, edge-to-edge ready.
 *
 * See `docs/DECISIONS/0001-tech-stack.md` for the rationale.
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jlleitschuh.gradle.ktlint")
            apply("io.gitlab.arturbosch.detekt")
            apply("de.mannodermaus.android-junit5")
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                targetSdk = libs.intVersion("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = false
            }
            packaging {
                resources.excludes += setOf(
                    "/META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                )
            }
            buildTypes {
                getByName("debug") {
                    applicationIdSuffix = ".debug"
                    isMinifyEnabled = false
                }
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
            // Tally targets a single language (en-GB) and dark-only.
            // No light-mode resources will ship; verify in M6.
            sourceSets.named("main") {
                kotlin.srcDirs("src/main/kotlin")
            }
            sourceSets.named("test") {
                kotlin.srcDirs("src/test/kotlin")
            }
            sourceSets.named("androidTest") {
                kotlin.srcDirs("src/androidTest/kotlin")
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    listOf(
                        "-Xjvm-default=all",
                        "-Xcontext-receivers",
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    ),
                )
            }
        }
    }
}
