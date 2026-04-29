pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") // 🔥 también aquí
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // 🔥 CAMBIO
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // 🔥 correcto
    }
}

rootProject.name = "ACADEMIAM"
include(":app")