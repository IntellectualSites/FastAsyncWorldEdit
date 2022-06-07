applyPaperweightAdapterConfiguration()

plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}


dependencies {
    paperDevBundle("1.17.1-R0.1-20220414.034903-210")
    compileOnly("io.papermc:paperlib")
}
