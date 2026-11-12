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

rootProject.name = "MusicSocial"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// Core: shared building blocks. Features and data modules depend on these, never the reverse.
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:domain")
include(":core:navigation")
include(":core:network")
include(":core:playback")
include(":core:sample") // TESTING ONLY: hand-written sample content, delete once repositories are real
include(":core:testing")
include(":core:ui")

// Data: repository implementations and data sources, one module per bounded context.
include(":data:auth")
include(":data:chat")
include(":data:music")
include(":data:user")

// Features: presentation only (screens, ViewModels, UI state). Features never depend on each other.
include(":feature:auth")
include(":feature:chat")
include(":feature:discover")
include(":feature:home")
include(":feature:music")
include(":feature:player")
include(":feature:profile")
