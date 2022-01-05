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
    paperDevBundle("1.18.1-R0.1-20211221.093324-19")
    compileOnly(libs.paperlib)
}
