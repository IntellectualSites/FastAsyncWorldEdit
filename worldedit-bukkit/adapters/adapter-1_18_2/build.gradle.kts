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
    paperDevBundle("1.18.2-R0.1-20220607.005826-161")
    compileOnly("io.papermc:paperlib")
}
