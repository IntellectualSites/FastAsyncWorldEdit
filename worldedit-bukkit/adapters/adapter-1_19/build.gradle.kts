plugins {
    java
}

applyPaperweightAdapterConfiguration()

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    compileOnly("io.papermc:paperlib")
}
