applyLibrariesConfiguration()

dependencies {
    "shade"("net.kyori:text-api:3.0.4")
    "shade"("net.kyori:text-serializer-gson:3.0.4")
    "shade"("net.kyori:text-serializer-legacy:3.0.4")
    "shade"("net.kyori:text-serializer-plain:3.0.4")
    "shade"("com.sk89q:jchronic:0.2.4a") {
        exclude(group = "junit", module = "junit")
    }
    "shade"("com.thoughtworks.paranamer:paranamer:2.8")
    "shade"("com.sk89q.lib:jlibnoise:1.0.0")
    "shade"("org.enginehub.piston:core:${Versions.PISTON}")
    "shade"("org.enginehub.piston.core-ap:runtime:${Versions.PISTON}")
    "shade"("org.enginehub.piston:default-impl:${Versions.PISTON}")
    "shade"("net.kyori:adventure-nbt:4.8.1")
}
