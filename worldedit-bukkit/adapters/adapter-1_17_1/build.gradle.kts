// TODO await https://github.com/PaperMC/paperweight/issues/116
//applyPaperweightAdapterConfiguration()
//
//dependencies {
//    paperDevBundle("1.17.1-R0.1-20211120.192557-194")
//}

// Until the above issue is resolved, we are bundling old versions using their last assembled JAR.
// Technically this means we cannot really update them, but that is is the price we pay for supporting older versions.


plugins {
    base
    java
}

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    maven {
        name = "Athion"
        url = uri("https://ci.athion.net/plugin/repository/tools/")
        content {
            includeModule("io.papermc", "paper-server")
        }
    }
    maven {
        name = "OSS Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        name = "Mojang"
        url = uri("https://libraries.minecraft.net/")
    }
}

artifacts {
    add("default", file("./src/main/resources/worldedit-adapter-1.17.1.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly(project(":worldedit-bukkit"))
    compileOnly(project(":worldedit-core"))
    compileOnly("io.papermc:paper-server:1_17_r1_2")
    compileOnly(libs.paperlib)
    compileOnly("com.mojang:datafixerupper:4.0.26")
    compileOnly("com.mojang:authlib:2.3.31")
    compileOnly("com.mojang:brigadier:1.0.18")
}
