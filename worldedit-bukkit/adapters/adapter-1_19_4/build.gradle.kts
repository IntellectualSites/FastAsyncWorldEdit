plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperDevBundle("1.19.4-R0.1-20230423.020222-72")
    compileOnly("io.papermc:paperlib")
}
