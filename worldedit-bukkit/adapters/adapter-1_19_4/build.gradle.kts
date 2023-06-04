import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.19.4-R0.1-20230601.025018-99")
    compileOnly("io.papermc:paperlib")
}
