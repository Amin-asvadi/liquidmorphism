plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

kotlin {
    android {
        namespace = "com.liquidmorphism.glass"
        compileSdk = 36
        minSdk = 21
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:${libs.versions.composePlugin.get()}")
            api("org.jetbrains.compose.foundation:foundation:${libs.versions.composePlugin.get()}")
            api("org.jetbrains.compose.ui:ui:${libs.versions.composePlugin.get()}")
        }

        androidMain.dependencies {}
    }
}

group = "com.github.Amin-asvadi.liquidmorphism"
version = "1.0.4"

publishing {
    publications.withType<MavenPublication>().configureEach {
        groupId = project.group.toString()
        version = project.version.toString()
    }
}
