import java.util.Properties

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.3.61"
}

repositories {
    jcenter()
    gradlePluginPortal()
}

configurations.all {
    resolutionStrategy {
        // Fabric needs this.
        force(
            "commons-io:commons-io:2.5",
            "org.ow2.asm:asm:7.1",
            "org.ow2.asm:asm-commons:7.1"
        )
    }
}

val properties = Properties().also { props ->
    project.projectDir.resolveSibling("gradle.properties").bufferedReader().use {
        props.load(it)
    }
}
val loomVersion: String = properties.getProperty("loom.version")
val mixinVersion: String = properties.getProperty("mixin.version")

dependencies {
    implementation(gradleApi())
    implementation("org.ajoberstar.grgit:grgit-gradle:3.1.1")
    implementation("com.github.jengelman.gradle.plugins:shadow:5.1.0")
    implementation("net.ltgt.apt-eclipse:net.ltgt.apt-eclipse.gradle.plugin:0.21")
    implementation("net.ltgt.apt-idea:net.ltgt.apt-idea.gradle.plugin:0.21")
    implementation("gradle.plugin.com.mendhak.gradlecrowdin:plugin:0.1.0")
}
