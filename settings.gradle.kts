rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

listOf("bukkit", "core").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}
include("worldedit-libs:core:ap")

plugins {
    id("com.gradle.enterprise").version("3.1.1")
}
