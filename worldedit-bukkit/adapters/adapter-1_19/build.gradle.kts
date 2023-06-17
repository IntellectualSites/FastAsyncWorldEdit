import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.19.2-R0.1-20221206.184705-189")
    compileOnly("io.papermc:paperlib")
}
