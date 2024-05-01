import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.20.6-R0.1-SNAPSHOT/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.20.6-R0.1-20240501.000126-8")
    compileOnly(libs.paperlib)
}
