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
    // https://papermc.io/repo/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/
    paperDevBundle("1.18.2-R0.1-20220703.182221-166")
    compileOnly("io.papermc:paperlib")
}
