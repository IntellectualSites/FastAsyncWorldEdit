import buildlogic.getVersion
import buildlogic.stringyLibs

plugins {
    `java-library`
    id("buildlogic.common")
    id("buildlogic.common-java")
    id("io.papermc.paperweight.userdev")
}

repositories {
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "FabricMC (Yarn)"
        url = uri("https://maven.fabricmc.net/#yarn-only")
    }
    mavenCentral()
    afterEvaluate {
        killNonEngineHubRepositories()
    }
}

dependencies {
    implementation(project(":worldedit-bukkit"))
    constraints {
        //Reduces the amount of libraries Gradle and IntelliJ need to resolve
        implementation("net.kyori:adventure-bom") {
            version { strictly(stringyLibs.getVersion("adventure").strictVersion) }
            because("Ensure a consistent version of adventure is used.")
        }
        "remapper"("net.fabricmc:tiny-remapper:[${stringyLibs.getVersion("minimumTinyRemapper")},)") {
            because("Need remapper to support Java 21")
        }
    }
}

java {
    // Required when we de-sync release option and declared Java versions.
    disableAutoTargetJvm()
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
