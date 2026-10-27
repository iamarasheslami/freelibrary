import java.net.URI
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:${libs.versions.kotlinxSerialization.get()}")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${libs.versions.kotlinxSerialization.get()}")
    }
}

// Release signing details live in keystore.properties at the repository root (never committed; see
// CONTRIBUTING.md). Without that file the release build is produced unsigned, as it is in CI.
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.isFile) file.inputStream().use { load(it) }
    }

android {
    namespace = "com.freelibrary.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.freelibrary.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // Dependency and Gradle version updates are handled by Dependabot.
        disable += setOf("NewerVersionAvailable", "AndroidGradlePluginVersion")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
            }
        }
    }
}

dependencies {
    // Shared sync data contract
    implementation(project(":shared"))

    // Core / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Settings storage (Preferences DataStore)
    implementation(libs.androidx.datastore.preferences)

    // Paged book lists (the "View more" pages)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.runtime)

    // Cover images (Coil 2.x: Views-friendly, capped memory and disk caches)
    implementation(libs.coil)
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Testing (local unit tests)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
}
tasks.register("fetchCatalogDatabase") {
    group = "freelibrary"
    description = "Downloads the bundled catalog database from GitHub Releases into app/src/main/assets/."

    val releaseTag = "catalog-2026-10-02"
    val expectedSha256 = "CE266D3857478193A82FA61A93613F5861A90E12E74611953D387217BB4BFB5E"
    val downloadUrl = "https://github.com/iamarasheslami/freelibrary/releases/download/$releaseTag/catalog.sqlite"
    val assetsDir = layout.projectDirectory.dir("src/main/assets")
    val outputFile = assetsDir.file("catalog.sqlite").asFile

    doLast {
        outputFile.parentFile.mkdirs()

        println("Downloading catalog database from $downloadUrl ...")
        URI(downloadUrl).toURL().openStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val actualHashBytes =
            outputFile.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest()
            }
        val actualSha256 = actualHashBytes.joinToString("") { "%02X".format(it) }

        if (actualSha256 != expectedSha256) {
            outputFile.delete()
            throw GradleException(
                "Downloaded catalog database checksum mismatch. Expected $expectedSha256 but got $actualSha256. " +
                    "The file has been deleted; do not trust a corrupted or tampered database.",
            )
        }

        println("Catalog database downloaded and verified successfully: $outputFile")
    }
}
