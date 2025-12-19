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
    // TODO: switch back to REOBF when paper releases mappings
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
        content {
            excludeModule("io.papermc.paper", "dev-bundle")
        }
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            excludeModule("io.papermc.paper", "dev-bundle")
        }
    }
    maven {
        name = "IntellectualSites"
        url = uri("https://repo.intellectualsites.dev/repository/paper-dev-bundles/")
        content {
            includeModule("io.papermc.paper", "dev-bundle")
        }
    }
    maven {
        name = "TCodedReleases"
        url = uri("https://repo.tcoded.com/releases")
    }
    mavenCentral()
    afterEvaluate {
        killNonEngineHubRepositories()
    }
}

dependencies {
    implementation(project(":worldedit-bukkit"))
    compileOnly("com.tcoded:FoliaLib:0.5.1")
    constraints {
        //Reduces the amount of libraries Gradle and IntelliJ need to resolve
        implementation("net.kyori:adventure-bom") {
            version { strictly(stringyLibs.getVersion("adventure").strictVersion) }
            because("Ensure a consistent version of adventure is used.")
        }
    }
}

// TODO: re-enable when paper releases mappings
/* tasks.named("assemble") {
    dependsOn("reobfJar")
} */

tasks.named<Javadoc>("javadoc") {
    enabled = false
}
