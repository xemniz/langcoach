rootProject.name = "langcoach"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Compose Multiplatform dev artifacts (only if using EAPs — leave commented in stable)
        // maven("https://redirector.kotlinlang.org/maven/compose-dev")
    }
}

plugins {
    // Foojay toolchain resolver — lets Gradle auto-download JDKs by version.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

// Application modules
include(":androidApp")
include(":composeApp")

// Shared modules
include(":shared:core")
include(":shared:data")
include(":shared:domain")
include(":shared:llm")
include(":shared:voice")

// iosApp is an Xcode project, not a Gradle module. Lives at /iosApp.
