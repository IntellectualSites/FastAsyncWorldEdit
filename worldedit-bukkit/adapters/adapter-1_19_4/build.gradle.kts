plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperDevBundle("1.19.4-R0.1-20230317.182750-6")
    compileOnly("io.papermc:paperlib")
}
