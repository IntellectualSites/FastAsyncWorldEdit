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
    paperDevBundle("1.18.1-R0.1-20220228.153921-147")
    compileOnly(libs.paperlib)
}
