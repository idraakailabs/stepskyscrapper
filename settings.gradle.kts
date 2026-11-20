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

rootProject.name = "StepSkyscraper"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":baselineprofile")
include(":core:designsystem")
include(":core:ui")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:steps")
include(":core:billing")
include(":core:ads")
include(":core:analytics")
include(":core:testing")
include(":feature:onboarding")
include(":feature:build")
include(":feature:skyline")
include(":feature:customise")
include(":feature:me")
include(":feature:share")
include(":feature:paywall")
include(":widget")
