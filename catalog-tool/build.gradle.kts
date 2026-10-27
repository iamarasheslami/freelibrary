plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.commons.compress)
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.junit)
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

tasks.register<JavaExec>("nightlySync") {
    group = "application"
    description = "Fetches today's RSS feed and updates the export data for books that are new or changed."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.freelibrary.catalogtool.NightlySyncKt")
}
