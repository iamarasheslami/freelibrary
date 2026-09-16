plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.serialization")
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

tasks.register<JavaExec>("inspect") {
    group = "application"
    description = "Analyzes the already-generated catalog.sqlite without re-running the full pipeline."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.freelibrary.catalogtool.InspectKt")
}
