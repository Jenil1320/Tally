package com.tally.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Applies Hilt + KSP and wires the Hilt compiler dependencies. The Hilt
 * Android library and the navigation/work extensions are added so any module
 * applying this plugin gets DI for free.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.dagger.hilt.android")
            apply("com.google.devtools.ksp")
        }

        dependencies {
            add("implementation", libs.lib("hilt-android"))
            add("ksp", libs.lib("hilt-compiler"))
            add("implementation", libs.lib("hilt-navigation-compose"))
            add("implementation", libs.lib("hilt-work"))
            add("ksp", libs.lib("hilt-work-compiler"))
            add("kspAndroidTest", libs.lib("hilt-compiler"))
            add("androidTestImplementation", libs.lib("hilt-testing"))
        }
    }
}
