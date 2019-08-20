applyLibrariesConfiguration()

dependencies {
    "shade"("net.kyori:text-api:${Versions.TEXT}")
    "shade"("net.kyori:text-serializer-gson:${Versions.TEXT}")
    "shade"("net.kyori:text-serializer-legacy:${Versions.TEXT}")
    "shade"("net.kyori:text-serializer-plain:${Versions.TEXT}")
    "shade"("com.sk89q:jchronic:0.2.4a") {
        exclude(group = "junit", module = "junit")
    }
    "shade"("com.thoughtworks.paranamer:paranamer:2.6")
    "shade"("com.sk89q.lib:jlibnoise:1.0.0")
    "shade"("FAWE-Piston:core/build/libs/core-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/runtime/build/libs/runtime-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:default-impl/build/libs/default-impl-${Versions.PISTON}:lastSuccessfulBuild@jar")
}
