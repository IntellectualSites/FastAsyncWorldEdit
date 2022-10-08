import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.attribute.Obfuscation

plugins {
    `java-library`
    id("com.modrinth.minotaur") version "2.+"
}

project.description = "Bukkit"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "EngineHub"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "GriefDefender"
        url = uri("https://repo.glaremasters.me/repository/bloodshot/")
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

val adapters = configurations.create("adapters") {
    description = "Adapters to include in the JAR"
    isCanBeConsumed = false
    isCanBeResolved = true
    shouldResolveConsistentlyWith(configurations["runtimeClasspath"])
    attributes {
        attribute(Obfuscation.OBFUSCATION_ATTRIBUTE, objects.named(Obfuscation.OBFUSCATED))
    }
}

dependencies {
    // Modules
    api(projects.worldeditCore)
    api(projects.worldeditLibs.bukkit)

    project.project(":worldedit-bukkit:adapters").subprojects.forEach {
        "adapters"(project(it.path))
    }

    // Minecraft expectations
    implementation(libs.fastutil)

    // Platform expectations
    compileOnly("io.papermc.paper:paper-api") {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // Logging
    localImplementation("org.apache.logging.log4j:log4j-api")
    localImplementation(libs.log4jBom) {
        because("Spigot provides Log4J (sort of, not in API, implicitly part of server)")
    }

    // Plugins
    compileOnly("com.github.MilkBowl:VaultAPI") { isTransitive = false }
    compileOnly(libs.dummypermscompat) {
        exclude("com.github.MilkBowl", "VaultAPI")
    }
    compileOnly(libs.worldguard) {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldedit.worldedit-libs", "bukkit")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    compileOnly(libs.mapmanager) { isTransitive = false }
    compileOnly(libs.griefprevention) { isTransitive = false }
    compileOnly(libs.griefdefender) { isTransitive = false }
    compileOnly(libs.mcore) { isTransitive = false }
    compileOnly(libs.residence) { isTransitive = false }
    compileOnly(libs.towny) { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Bukkit") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Core") { isTransitive = false }

    // Third party
    implementation("io.papermc:paperlib")
    implementation("org.bstats:bstats-bukkit") { isTransitive = false }
    implementation(libs.bstatsBase) { isTransitive = false }
    implementation("dev.notmyfault.serverlib:ServerLib")
    implementation("com.intellectualsites.paster:Paster") { isTransitive = false }
    api(libs.lz4Java) { isTransitive = false }
    api(libs.sparsebitset) { isTransitive = false }
    api(libs.parallelgzip) { isTransitive = false }
    compileOnly("net.kyori:adventure-api")
    compileOnlyApi("org.checkerframework:checker-qual")

    // Tests
    testImplementation(libs.mockito)
    testImplementation("net.kyori:adventure-api")
    testImplementation("org.checkerframework:checker-qual")
    testImplementation("io.papermc.paper:paper-api") { isTransitive = true }
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand("internalVersion" to internalVersion)
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}

addJarManifest(WorldEditKind.Plugin, includeClasspath = true)

tasks.named<ShadowJar>("shadowJar") {
    dependsOn(project.project(":worldedit-bukkit:adapters").subprojects.map { it.tasks.named("assemble") })
    from(Callable {
        adapters.resolve()
                .map { f ->
                    zipTree(f).matching {
                        exclude("META-INF/")
                    }
                }
    })
    archiveFileName.set("${rootProject.name}-Bukkit-${project.version}.${archiveExtension.getOrElse("jar")}")
    dependencies {
        // In tandem with not bundling log4j, we shouldn't relocate base package here.
        // relocate("org.apache.logging", "com.sk89q.worldedit.log4j")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:bukkit"))
        // Purposefully not included, we assume (even though no API exposes it) that Log4J will be present at runtime
        // If it turns out not to be true for Spigot/Paper, our only two official platforms, this can be uncommented.
        // include(dependency("org.apache.logging.log4j:log4j-api"))
        include(dependency("org.antlr:antlr4-runtime"))
        // ZSTD does not work if relocated. https://github.com/luben/zstd-jni/issues/189 Use not latest as it can be difficult
        // to obtain latest ZSTD lib
        include(dependency("com.github.luben:zstd-jni:1.4.8-1"))
        relocate("org.bstats", "com.sk89q.worldedit.bstats") {
            include(dependency("org.bstats:"))
        }
        relocate("io.papermc.lib", "com.sk89q.worldedit.bukkit.paperlib") {
            include(dependency("io.papermc:paperlib"))
        }
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
        relocate("org.incendo.serverlib", "com.fastasyncworldedit.serverlib") {
            include(dependency("dev.notmyfault.serverlib:ServerLib:2.3.1"))
        }
        relocate("com.intellectualsites.paster", "com.fastasyncworldedit.paster") {
            include(dependency("com.intellectualsites.paster:Paster"))
        }
        relocate("org.lz4", "com.fastasyncworldedit.core.lz4") {
            include(dependency("org.lz4:lz4-java:1.8.0"))
        }
        relocate("net.kyori", "com.fastasyncworldedit.core.adventure") {
            include(dependency("net.kyori:adventure-nbt:4.9.3"))
        }
        relocate("com.zaxxer", "com.fastasyncworldedit.core.math") {
            include(dependency("com.zaxxer:SparseBitSet:1.2"))
        }
        relocate("org.anarres", "com.fastasyncworldedit.core.internal.io") {
            include(dependency("org.anarres:parallelgzip:1.0.5"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

tasks {
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("fastasyncworldedit")
        versionName.set("${project.version}")
        versionNumber.set("${project.version}")
        versionType.set("release")
        uploadFile.set(file("build/libs/${rootProject.name}-Bukkit-${project.version}.jar"))
        gameVersions.addAll(listOf("1.19.2", "1.19.1", "1.19", "1.18.2", "1.17.1", "1.16.5"))
        loaders.addAll(listOf("paper", "purpur", "spigot"))
        changelog.set("The changelog is available on GitHub: https://github.com/IntellectualSites/" +
                "FastAsyncWorldEdit/releases/tag/${project.version}")
        syncBodyFrom.set(rootProject.file("README.md").readText())
    }
}
