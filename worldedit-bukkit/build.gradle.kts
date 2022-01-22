import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.attribute.Obfuscation

plugins {
    `java-library`
}

project.description = "Bukkit"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://papermc.io/repo/repository/maven-public/")
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
        name = "ProtocolLib"
        url = uri("https://repo.dmulloy2.net/nexus/repository/public/")
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
    compileOnly(libs.paper) {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // Logging
    localImplementation(libs.log4j)
    localImplementation(libs.log4jBom) {
        because("Spigot provides Log4J (sort of, not in API, implicitly part of server)")
    }

    // Plugins
    compileOnly(libs.vault) { isTransitive = false }
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
    compileOnly(libs.protocollib) { isTransitive = false }
    compileOnly(libs.plotsquaredV6Bukkit) { isTransitive = false }
    compileOnly(libs.plotsquaredV6Core) { isTransitive = false }

    // Third party
    implementation(libs.paperlib)
    implementation(libs.bstatsBukkit) { isTransitive = false }
    implementation(libs.bstatsBase) { isTransitive = false }
    implementation(libs.serverlib)
    api(libs.paster) { isTransitive = false }
    api(libs.lz4Java) { isTransitive = false }
    api(libs.sparsebitset) { isTransitive = false }
    api(libs.parallelgzip) { isTransitive = false }
    compileOnly(libs.adventure)
    compileOnlyApi(libs.checkerqual)

    // Tests
    testImplementation(libs.mockito)
    testImplementation(libs.adventure)
    testImplementation(libs.checkerqual)
    testImplementation(libs.paper) { isTransitive = true }
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
            include(dependency("com.intellectualsites.paster:Paster:1.1.3"))
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
