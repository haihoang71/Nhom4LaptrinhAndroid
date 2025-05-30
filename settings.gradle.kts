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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Đã sửa cú pháp Kotlin DSL
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Modified_expensify"
include(":app")
include(":app:modified_expensify")