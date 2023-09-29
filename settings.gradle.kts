rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

listOf("legacy", "1_17_1", "1_18_2", "1_19_4", "1_20", "1_20_2").forEach {
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
