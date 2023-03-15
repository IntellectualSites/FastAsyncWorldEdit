plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
    implementation(project(mapOf("path" to ":worldedit-bukkit:adapters:adapter-1_19_3")))
    // https://papermc.io/repo/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/
    paperDevBundle("1.19.4-R0.1-SNAPSHOT") // Todo: Update change from local to remote
    compileOnly("io.papermc:paperlib")
}
