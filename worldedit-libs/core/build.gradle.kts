plugins {
    `java-base`
}

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
    "shade"(libs.adventureNbt)
}
