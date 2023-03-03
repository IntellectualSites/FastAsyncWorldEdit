applyLibrariesConfiguration()

dependencies {
    "shade"(libs.adventureTextApi)
    "shade"(libs.adventureTextSerializerGson)
    "shade"(libs.adventureTextSerializerLegacy)
    "shade"(libs.adventureTextSerializerPlain)
    "shade"(libs.piston)
    "shade"(libs.pistonRuntime)
    "shade"(libs.pistonImpl)
    // Linbus
    "shade"("org.enginehub.lin-bus:lin-bus-common:0.1.0-SNAPSHOT")
    "shade"("org.enginehub.lin-bus:lin-bus-stream:0.1.0-SNAPSHOT")
    "shade"("org.enginehub.lin-bus:lin-bus-tree:0.1.0-SNAPSHOT")
    "shade"("org.enginehub.lin-bus.format:lin-bus-format-snbt:0.1.0-SNAPSHOT")
}
