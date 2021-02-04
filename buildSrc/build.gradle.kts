import java.util.Properties

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

repositories {
    jcenter()
    gradlePluginPortal()
    /*
    maven {
        name = "Forge Maven"
        url = uri("https://files.minecraftforge.net/maven")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven")
    }
     */
    maven {
        name = "EngineHub"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            includeGroupByRegex("com.sk89q.*")
        }
    }
}
/*
configurations.all {
    resolutionStrategy {
        // Fabric needs this.
        force(
            "commons-io:commons-io:2.6",
            "org.ow2.asm:asm:8.0.1",
            "org.ow2.asm:asm-commons:8.0.1"
        )
    }
}
 */

val properties = Properties().also { props ->
    project.projectDir.resolveSibling("gradle.properties").bufferedReader().use {
        props.load(it)
    }
}

dependencies {
    implementation(gradleApi())
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.5.0")
    implementation("org.ajoberstar.grgit:grgit-gradle:4.1.0")
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    implementation("net.ltgt.apt-eclipse:net.ltgt.apt-eclipse.gradle.plugin:0.21")
    implementation("net.ltgt.apt-idea:net.ltgt.apt-idea.gradle.plugin:0.21")
    /*
    implementation("gradle.plugin.com.mendhak.gradlecrowdin:plugin:0.1.0")
    implementation("org.enginehub.gradle:gradle-codecov-plugin:0.1.0")
    implementation("org.jfrog.buildinfo:build-info-extractor-gradle:4.16.0")
    implementation("gradle.plugin.org.spongepowered:spongegradle:0.9.0")
    implementation("net.minecraftforge.gradle:ForgeGradle:3.0.181")
    implementation("net.fabricmc:fabric-loom:$loomVersion")
    implementation("net.fabricmc:sponge-mixin:$mixinVersion")
     */
}
