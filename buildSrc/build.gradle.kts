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
    implementation("org.ajoberstar.grgit:grgit-gradle:5.2.2")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("io.papermc.paperweight.userdev:io.papermc.paperweight.userdev.gradle.plugin:1.7.2")
    constraints {
        val asmVersion = "[9.7,)"
        implementation("org.ow2.asm:asm:$asmVersion") {
            because("Need Java 21 support in shadow")
        }
        implementation("org.ow2.asm:asm-commons:$asmVersion") {
            because("Need Java 21 support in shadow")
        }
        implementation("org.vafer:jdependency:[2.10,)") {
            because("Need Java 21 support in shadow")
        }
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(21))
    }
}
