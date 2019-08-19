rootProject.name = "FastAsyncWorldEdit"

include("worldedit-libs")

listOf("core", "bukkit").forEach {
    include("worldedit-libs:$it")
    include("worldedit-$it")
}
include("worldedit-libs:core:ap")
