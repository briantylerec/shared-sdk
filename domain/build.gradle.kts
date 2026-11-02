import com.redbull.lab.build.LibraryCoordinates

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

kotlin { jvmToolchain(17) }

java { withSourcesJar() }

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = LibraryCoordinates.GROUP_ID
            artifactId = LibraryCoordinates.ANDROID_DOMAIN_ARTIFACT_ID
            version = providers.gradleProperty("releaseVersion").orElse(LibraryCoordinates.VERSION_NAME).get()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPOSITORY"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
