plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperweightDevBundle("dev.folia", "1.19.4-R0.1-SNAPSHOT")
    //paperDevBundle("1.19.4-R0.1-20230317.182750-6")
    compileOnly("io.papermc:paperlib")
}
