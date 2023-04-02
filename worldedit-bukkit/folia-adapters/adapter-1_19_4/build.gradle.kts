plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperweightDevBundle("dev.folia", "1.19.4-R0.1-SNAPSHOT")
    compileOnly("io.papermc:paperlib")
}
