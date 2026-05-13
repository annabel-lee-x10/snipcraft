pluginManagement {
    includeBuild("build-logic")
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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "snipcraft"

include(":app")

// core modules
include(":core:common")
include(":core:domain")
include(":core:engine")
include(":core:variables")
include(":core:accessibility")
include(":core:compatibility")
include(":core:database")
include(":core:data")
include(":core:backup")
include(":core:sync")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

// feature modules
include(":feature:onboarding")
include(":feature:library")
include(":feature:editor")
include(":feature:settings")
include(":feature:diagnostics")
include(":feature:quickadd")
