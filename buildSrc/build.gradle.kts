import java.util.Properties

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        name = "EngineHub"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

val properties = Properties().also { props ->
    project.projectDir.resolveSibling("gradle.properties").bufferedReader().use {
        props.load(it)
    }
}

dependencies {
    implementation(gradleApi())
    implementation("org.ajoberstar.grgit:grgit-gradle:5.2.1")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:1.5.11")
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}
