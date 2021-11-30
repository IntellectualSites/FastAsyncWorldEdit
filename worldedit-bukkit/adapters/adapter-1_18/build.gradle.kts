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
    paperDevBundle("1.18-rc3-R0.1-20211129.221606-5")
    compileOnly(libs.paperlib)
    compileOnly(libs.paper)
}
