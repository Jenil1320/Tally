plugins {
    `kotlin-dsl`
}

group = "com.tally.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tally.android.application"
            implementationClass = "com.tally.buildlogic.AndroidAppConventionPlugin"
        }
        register("androidCompose") {
            id = "tally.android.compose"
            implementationClass = "com.tally.buildlogic.ComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "tally.android.hilt"
            implementationClass = "com.tally.buildlogic.HiltConventionPlugin"
        }
    }
}
