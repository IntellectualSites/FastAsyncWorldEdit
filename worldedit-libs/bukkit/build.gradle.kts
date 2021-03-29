applyLibrariesConfiguration()
constrainDependenciesToLibsCore()

repositories {
    maven {
        name = "SpigotMC"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

dependencies {
    "shade"("net.kyori:text-adapter-bukkit:3.0.6")
}
