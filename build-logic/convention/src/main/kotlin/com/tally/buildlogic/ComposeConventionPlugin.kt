package com.tally.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose on a module already configured by
 * [AndroidAppConventionPlugin], wires the Compose BOM and the Tally
 * baseline UI dependencies, and pulls debug-only tooling.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = libs.lib("compose-bom")
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))

            // Bundled UI deps — full list lives in libs.versions.toml.
            libs.findBundle("compose-app").orElseThrow {
                IllegalStateException("Missing bundle: compose-app")
            }.get().forEach { add("implementation", it) }

            libs.findBundle("compose-debug").orElseThrow {
                IllegalStateException("Missing bundle: compose-debug")
            }.get().forEach { add("debugImplementation", it) }

            add("androidTestImplementation", libs.lib("compose-ui-test-junit4"))
        }
    }
}
