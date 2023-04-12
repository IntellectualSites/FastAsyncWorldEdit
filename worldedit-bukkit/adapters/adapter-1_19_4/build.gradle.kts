plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperDevBundle("1.19.4-R0.1-20230412.010331-64")
    compileOnly("io.papermc:paperlib")
}
