pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
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
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "spinroom-player"

include(":app")
include(":core:design", ":core:sendspin", ":core:audio", ":core:ma-api", ":core:data")
include(
    ":feature:nowplaying", ":feature:home", ":feature:library", ":feature:details",
    ":feature:sources", ":feature:search", ":feature:settings",
)
