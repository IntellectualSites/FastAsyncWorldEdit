plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    gradlePluginPortal()
}

dependencies {
    paperDevBundle("1.19.4-R0.1-20230331.112431-38")
    compileOnly("io.papermc:paperlib")
}
