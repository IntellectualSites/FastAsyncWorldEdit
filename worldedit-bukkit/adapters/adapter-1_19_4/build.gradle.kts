plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperDevBundle("1.19.4-R0.1-20230315.180941-2")
    compileOnly("io.papermc:paperlib")
}
