import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.20.4-R0.1-SNAPSHOT
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.20.4-R0.1-20240316.193646-135")
    compileOnly(libs.paperlib)
}
