applyPaperweightAdapterConfiguration()

plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}


dependencies {
    paperDevBundle("1.17.1-R0.1-20211219.175449-201")
    compileOnly(libs.paperlib)
}
