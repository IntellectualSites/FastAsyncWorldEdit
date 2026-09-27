import buildlogic.getVersion
import buildlogic.stringyLibs

plugins {
    `java-library`
    id("buildlogic.common")
    id("buildlogic.common-java")
    id("io.papermc.paperweight.userdev")
}

paperweight {
    injectPaperRepository = false
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            excludeModule("net.fabricmc", "yarn")
        }
    }
    maven {
        name = "IntellectualSites"
        url = uri("https://repo.intellectualsites.dev/repository/maven-all/")
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
    }
}

java {
    // Required when we de-sync release option and declared Java versions.
    disableAutoTargetJvm()
}

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
