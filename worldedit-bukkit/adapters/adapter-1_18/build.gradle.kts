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
    paperDevBundle("1.18-R0.1-20211210.020702-61")
    compileOnly(libs.paperlib)
}
