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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // VietMap SDK chỉ có trên JitPack; giới hạn nhóm để Gradle không hỏi JitPack cho thư viện khác
        maven("https://jitpack.io") {
            content { includeGroup("com.github.vietmap-company") }
        }
    }
}

rootProject.name = "Lokap"
include(":app")
