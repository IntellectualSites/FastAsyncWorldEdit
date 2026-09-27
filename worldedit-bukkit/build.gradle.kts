import buildlogic.sourceSets
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.attribute.Obfuscation
import me.modmuss50.mpp.ReleaseType

plugins {
    `java-library`
    id("buildlogic.platform")
    alias(libs.plugins.mod.publish.plugin)
}

project.description = "Bukkit"

platform {
    kind = buildlogic.WorldEditKind.Plugin
    includeClasspath = true
}

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
    }
    mavenCentral()
    maven {
        // mirroring + caching from unstable third-party repositories for community plugins (partially limited by routing rules)
        // (currently Residence, GriefPrevention, GriefDefender, Towny)
        name = "IntellectualSites Repository"
        url = uri("https://repo.intellectualsites.dev/repository/maven-all/")
    }
    maven {
        name = "OSS Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    flatDir { dir(File("src/main/resources")) }
}

val localImplementation = configurations.create("localImplementation") {
    description = "Dependencies used locally, but provided by the runtime Bukkit implementation"
    isCanBeConsumed = false
    isCanBeResolved = false
}
configurations["compileOnly"].extendsFrom(localImplementation)
configurations["testImplementation"].extendsFrom(localImplementation)

val adapters = configurations.create("adapters") {
    description = "Adapters to include in the JAR (Mojmap)"
    isCanBeConsumed = false
    isCanBeResolved = true
    shouldResolveConsistentlyWith(configurations["runtimeClasspath"])
    attributes {
        attribute(Obfuscation.OBFUSCATION_ATTRIBUTE, objects.named(Obfuscation.NONE))
    }
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            capabilitiesResolution {
                withCapability("org.lz4:lz4-java") {
                    select(candidates.first {
                        (it.id as org.gradle.api.artifacts.component.ModuleComponentIdentifier).group == "at.yawk.lz4"
                    })
                }
            }
        }
    }
}

dependencies {
    api(project(":worldedit-core"))
    api(project(":worldedit-libs:bukkit"))

    localImplementation(libs.paperApi) {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    localImplementation(platform(libs.log4j.bom)) {
        because("Paper provides Log4J (sort of, not in API, implicitly part of server)")
    }
    localImplementation(libs.log4j.api)

    compileOnly(libs.vault) { isTransitive = false }
    compileOnly(libs.dummypermscompat) {
        exclude("com.github.MilkBowl", "VaultAPI")
    }
    implementation(libs.bstats.bukkit) { isTransitive = false }
    implementation(libs.bstats.base) { isTransitive = false }
    implementation(libs.fastutil)

    project.project(":worldedit-bukkit:adapters").subprojects.forEach {
        "adapters"(project(it.path))
    }
    compileOnly(libs.worldguard) {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldedit.worldedit-libs", "bukkit")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    compileOnly(libs.griefprevention) { isTransitive = false }
    compileOnly(libs.griefdefender) { isTransitive = false }
    compileOnly(libs.residence) { isTransitive = false }
    compileOnly(libs.towny) { isTransitive = false }
    compileOnly(libs.plotsquared.bukkit) { isTransitive = false }
    compileOnly(libs.plotsquared.core) { isTransitive = false }
    compileOnly(libs.guice)

    // Third party
    implementation(libs.serverlib)
    implementation(libs.paster) { isTransitive = false }
    api(libs.lz4Java) { isTransitive = false }
    api(libs.sparsebitset) { isTransitive = false }
    api(libs.parallelgzip) { isTransitive = false }
    compileOnly(libs.adventureApi)
    compileOnlyApi(libs.checkerqual)

    // Tests
    testImplementation(libs.mockito.core)
    testImplementation(libs.adventureApi)
    testImplementation(libs.checkerqual)
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("internalVersion" to internalVersion))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-Paper-${project.version}.${archiveExtension.getOrElse("jar")}")
    configurations.add(adapters)
    manifest {
        attributes(
            "paperweight-mappings-namespace" to "mojang"
        )
    }
}

tasks.withType<ShadowJar>().configureEach {
    dependencies {
        // In tandem with not bundling log4j, we shouldn't relocate base package here.
        // relocate("org.apache.logging", "com.sk89q.worldedit.log4j")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        exclude(dependency("$group:$name"))

        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:bukkit"))
        // Purposefully not included, we assume (even though no API exposes it) that Log4J will be present at runtime
        // If it turns out not to be true for Paper, our only official platform, this can be uncommented.
        // include(dependency("org.apache.logging.log4j:log4j-api"))
        include(dependency("org.antlr:antlr4-runtime"))

        exclude(dependency("$group:$name"))
        // ZSTD does not work if relocated. https://github.com/luben/zstd-jni/issues/189 Use not latest as it can be difficult
        // to obtain latest ZSTD lib
        include(dependency(libs.zstd))
        relocate("org.bstats", "com.sk89q.worldedit.bstats") {
            include(dependency(libs.bstats.bukkit))
            include(dependency(libs.bstats.base))
        }
        relocate("net.royawesome.jlibnoise", "com.sk89q.worldedit.jlibnoise") {
            include(dependency("com.sk89q.lib:jlibnoise"))
        }
        relocate("org.incendo.serverlib", "com.fastasyncworldedit.serverlib") {
            include(dependency(libs.serverlib))
        }
        relocate("com.intellectualsites.paster", "com.fastasyncworldedit.paster") {
            include(dependency(libs.paster))
        }
        include(dependency(libs.lz4Java))
        relocate("com.zaxxer", "com.fastasyncworldedit.core.math") {
            include(dependency(libs.sparsebitset))
        }
        relocate("org.anarres", "com.fastasyncworldedit.core.internal.io") {
            include(dependency(libs.parallelgzip))
        }
    }
    project.project(":worldedit-bukkit:adapters").subprojects.forEach {
        dependencies {
            include(dependency("${it.group}:${it.name}"))
        }
        minimize {
            exclude(dependency("${it.group}:${it.name}"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

publishMods {
    displayName.set("${project.version}")
    version.set("${project.version}")
    type.set(ReleaseType.STABLE)
    changelog.set("The changelog is available on GitHub: https://github.com/IntellectualSites/" +
            "FastAsyncWorldEdit/releases/tag/${project.version}")

    val common = modrinthOptions {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "z4HZZnLr"
        projectDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText
    }

    val mojmapPaperVersions = listOf("1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10",
            "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2")

    modrinth {
        from(common)
        file = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }
        minecraftVersions = mojmapPaperVersions
        modLoaders = listOf("paper")
    }

    // dryRun.set(true) // For testing
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}
