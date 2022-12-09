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
    paperDevBundle("1.19.2-R0.1-20221206.184705-189")
    compileOnly("io.papermc:paperlib")
}
