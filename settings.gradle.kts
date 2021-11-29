rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

include("worldedit-bukkit:adapters:adapter-legacy")
// include("worldedit-bukkit:adapters:adapter-1_17_1")
include("worldedit-bukkit:adapters:adapter-1_18")

listOf("bukkit", "core", "cli").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}
// include("worldedit-mod")
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

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
