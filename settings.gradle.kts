pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroup("com.google.dagger") // 🔽 Hilt 플러그인 사용 위해 추가
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("dagger.hilt.android.plugin") version "2.48"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cleanroom3"
include(":app")
