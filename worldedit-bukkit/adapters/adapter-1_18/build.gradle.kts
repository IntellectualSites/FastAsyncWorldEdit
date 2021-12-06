plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    paperDevBundle("1.18-R0.1-20211206.203248-51")
    compileOnly(libs.paperlib)
    compileOnly(libs.paper)
}
