plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.freelibrary.catalogtool.MainKt")
}

kotlin {
    jvmToolchain(21)
}
