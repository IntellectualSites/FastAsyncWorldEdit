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
    "shade"("com.github.luben:zstd-jni:1.4.8-6")
    "shade"("com.sk89q.lib:jlibnoise:1.0.0")
    "shade"("org.enginehub.piston:core:0.5.7")
    "shade"("org.enginehub.piston.core-ap:runtime:0.5.6")
    "shade"("org.enginehub.piston:default-impl:0.5.6")
}
