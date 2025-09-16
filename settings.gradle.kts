import kotlin.system.exitProcess

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "EngineHub Repository"
            url = uri("https://maven.enginehub.org/repo/")
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositories {
        maven {
            name = "EngineHub Repository"
            url = uri("https://maven.enginehub.org/repo/")
        }
        mavenCentral()
    }
}

if (!File("$rootDir/.git").exists()) {
    logger.lifecycle("""
    **************************************************************************************
    You need to fork and clone this repository! Don't download a .zip file.
    If you need assistance, consult the GitHub docs: https://docs.github.com/get-started/quickstart/fork-a-repo
    **************************************************************************************
    """.trimIndent()
    ).also { exitProcess(1) }
}

logger.lifecycle("""
*******************************************
 You are building FastAsyncWorldEdit!

 If you encounter trouble:
 1) Read COMPILING.adoc if you haven't yet
 2) Try running 'build' in a separate Gradle run
 3) Use gradlew and not gradle
 4) If you still need help, ask on Discord! https://discord.gg/intellectualsites

 Output files will be in [subproject]/build/libs
*******************************************
""")

rootProject.name = "FastAsyncWorldEdit"

includeBuild("build-logic")

include("worldedit-libs")

listOf("1_20_2", "1_20_4", "1_20_5", "1_21", "1_21_4", "1_21_5", "1_21_6").forEach {
    include("worldedit-bukkit:adapters:adapter-$it")
}

listOf("bukkit", "core", "cli").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}
include("worldedit-libs:core:ap")


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
