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
    paperDevBundle("1.19-R0.1-20220610.160059-2")
    compileOnly("io.papermc:paperlib")
}
