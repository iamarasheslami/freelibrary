import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    kotlin("kapt")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.0")
    }
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

    buildTypes {
        release {
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.8")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.8")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.58")
    kapt("com.google.dagger:hilt-compiler:2.58")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // Testing (local unit tests)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
tasks.register("fetchCatalogDatabase") {
    group = "freelibrary"
    description = "Downloads the bundled catalog database from GitHub Releases into app/src/main/assets/."

    val releaseTag = "catalog-2026-09-19"
    val expectedSha256 = "087BAFB489C998CB924DA19278E0B89F6016132A528D2DE07412B309DCF329D3"
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
