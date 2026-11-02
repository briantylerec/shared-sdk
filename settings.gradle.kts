pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); mavenLocal() }
}
rootProject.name = "shared-android-sdk"
include(":domain", ":data", ":sdk", ":sample")
