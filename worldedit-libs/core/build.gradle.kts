applyLibrariesConfiguration()

dependencies {
    "shade"(libs.adventureTextApi)
    "shade"(libs.adventureTextSerializerGson)
    "shade"(libs.adventureTextSerializerLegacy)
    "shade"(libs.adventureTextSerializerPlain)
    "shade"(libs.jchronic) {
        exclude(group = "junit", module = "junit")
    }
    "shade"(libs.jlibnoise)
    "shade"(libs.piston)
    "shade"(libs.pistonRuntime)
    "shade"(libs.pistonImpl)
    // Linbus
    "shade"(platform(libs.linBus.bom))
    "shade"(libs.linBus.common)
    "shade"(libs.linBus.stream)
    "shade"(libs.linBus.tree)
    "shade"(libs.linBus.format.snbt)
}
