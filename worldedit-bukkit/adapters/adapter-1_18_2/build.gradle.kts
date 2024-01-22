import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.18.2-R0.1-SNAPSHOT
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.18.2-R0.1-20220920.010157-167")
    compileOnly(libs.paperlib)
}
