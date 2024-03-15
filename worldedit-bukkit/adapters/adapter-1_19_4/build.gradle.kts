import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.19.4-R0.1-SNAPSHOT
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.19.4-R0.1-20230608.201059-104")
    compileOnly(libs.paperlib)
}
