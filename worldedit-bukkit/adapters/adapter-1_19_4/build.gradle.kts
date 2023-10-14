import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.19.4-R0.1-20230608.201059-104")
    compileOnly(libs.paperlib)
}
