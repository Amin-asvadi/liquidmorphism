pluginManagement {
    repositories {

        maven { url = uri("https://en-mirror.ir") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)


    repositories {
        maven { url = uri("https://en-mirror.ir") }
        google()
        mavenCentral()
    }
}

rootProject.name = "Glass"
include(":app")
include(":glass")
 
