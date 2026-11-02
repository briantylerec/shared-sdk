import com.redbull.lab.build.LibraryCoordinates

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.redbull.lab.data"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":domain"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = LibraryCoordinates.GROUP_ID
            artifactId = LibraryCoordinates.ANDROID_DATA_ARTIFACT_ID
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

afterEvaluate {
    publishing.publications.named<MavenPublication>("release") {
        from(components["release"])
    }
}
