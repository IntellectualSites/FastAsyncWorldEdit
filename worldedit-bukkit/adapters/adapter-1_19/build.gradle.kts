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
    paperDevBundle("1.19-R0.1-20220627.223747-42")
    compileOnly("io.papermc:paperlib")
}
