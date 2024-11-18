import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.21.3-R0.1-SNAPSHOT/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.21.3-R0.1-20241117.183222-36")
    compileOnly(libs.paperlib)
}
