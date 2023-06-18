rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

listOf("1_17_1", "1_18_2", "1_19_4", "1_20", "1_20_2").forEach {
    include("worldedit-bukkit:adapters:adapter-$it")
}
listOf("bukkit", "core", "cli").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}
include("worldedit-libs:core:ap")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
    }
}
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "jmp repository"
            url = uri("https://repo.jpenilla.xyz/snapshots")
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
