pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")

        // Ví dụ GitHub Packages (nếu bạn dùng)
        // maven {
        //     url = uri("https://maven.pkg.github.com/OWNER/REPO")
        //     credentials {
        //         username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
        //         password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        //     }
        // }
    }
}

rootProject.name = "SDK_Ads"
include(":app")
include(":sdk_ads")
