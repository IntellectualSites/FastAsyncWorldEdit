plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    // https://papermc.io/repo/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/
    paperDevBundle("1.19.3-R0.1-20221226.180038-55")
    compileOnly("io.papermc:paperlib")
}
