pluginManagement {
    repositories {
        maven { url = uri("https://maven.myket.ir/") }
        maven { url = uri("https://maven.devneeds.ir") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.myket.ir/") }
        maven { url = uri("https://maven.devneeds.ir") }
        google()
        mavenCentral()
    }
}

rootProject.name = "FreeLibrary"
include(":app")
include(":catalog-tool")
include(":shared")