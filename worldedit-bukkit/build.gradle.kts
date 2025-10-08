import buildlogic.getLibrary
import buildlogic.sourceSets
import buildlogic.stringyLibs
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import io.papermc.paperweight.userdev.attribute.Obfuscation
import me.modmuss50.mpp.ReleaseType
import kotlin.getOrElse

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
    maven {
        name = "Glaremasters"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") // TODO Remove when 4.17.0 is released
    maven {
        name = "Athion"
        url = uri("https://ci.athion.net/plugin/repository/tools/")
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

val adaptersReobf = configurations.create("adaptersReobf") {
    description = "Adapters to include in the JAR (Spigot-Mapped)"
    isCanBeConsumed = false
    isCanBeResolved = true
    shouldResolveConsistentlyWith(configurations["runtimeClasspath"])
    attributes {
        attribute(Obfuscation.OBFUSCATION_ATTRIBUTE, objects.named(Obfuscation.OBFUSCATED))
    }
    extendsFrom(adapters)
}

dependencies {
    api(project(":worldedit-core"))
    api(project(":worldedit-libs:bukkit"))

    localImplementation(libs.paperApi) {
        exclude("junit", "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    localImplementation(platform(libs.log4j.bom)) {
        because("Spigot provides Log4J (sort of, not in API, implicitly part of server)")
    }
    localImplementation(libs.log4j.api)

    implementation(libs.paperLib)
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

tasks.register<ShadowJar>("reobfShadowJar") {
    // The `fawe.properties` file from `worldedit-core` is not automatically
    // included, so we explicitly add the `worldedit-core` source set output.
    from(project(":worldedit-core").sourceSets.main.get().output)
    archiveFileName.set("${rootProject.name}-Bukkit-${project.version}.${archiveExtension.getOrElse("jar")}")
    configurations = listOf(
        project.configurations.runtimeClasspath.get(), // as is done by shadow for the default shadowJar
        adaptersReobf
    )
    relocate("com.sk89q.jchronic", "com.sk89q.worldedit.jchronic")

    dependencies {
        include(project(":worldedit-libs:core"))
        include(project(":worldedit-libs:${project.name.replace("worldedit-", "")}"))
        include(project(":worldedit-core"))
        include(dependency(libs.jchronic))
        exclude(dependency(libs.jsr305))
    }
    minimize {
        // jchronic uses reflection to load things, so we need to exclude it from minimizing
        exclude(dependency(libs.jchronic))
    }

    // as is done by shadow for the default shadowJar
    from(sourceSets.main.map { it.output })
    manifest.inheritFrom(tasks.jar.get().manifest)
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

    manifest {
        attributes(
            "FAWE-Plugin-Jar-Type" to "spigot"
        )
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-Paper-${project.version}.${archiveExtension.getOrElse("jar")}")
    configurations.add(adapters)
    manifest {
        attributes(
            "paperweight-mappings-namespace" to "mojang",
            "FAWE-Plugin-Jar-Type" to "mojang"
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
        // If it turns out not to be true for Spigot/Paper, our only two official platforms, this can be uncommented.
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
        relocate("io.papermc.lib", "com.sk89q.worldedit.bukkit.paperlib") {
            include(dependency("io.papermc:paperlib"))
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
    dependsOn("reobfShadowJar")
}

publishMods {
    displayName.set("${project.version}")
    version.set("${project.version}")
    type.set(ReleaseType.STABLE)
    changelog.set("The changelog is available on GitHub: https://github.com/IntellectualSites/" +
            "FastAsyncWorldEdit/releases/tag/${project.version}")

    val common = modrinthOptions {
        accessToken.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("z4HZZnLr")
        projectDescription.set(providers.fileContents { layout.projectDirectory.file("README.md") as File }.asText)
    }

    // We publish the reobfJar twice to ensure that the modrinth download menu picks the right jar for the platform regardless
    // of minecraft version.

    val mojmapPaperVersions = listOf("1.20.6", "1.21.7", "1.21.8")
    val spigotMappedPaperVersions = listOf("1.20.2", "1.20.4")

    // Mark reobfJar as spigot only for 1.20.5+
    modrinth("spigot") {
        from(common)
        file.set(tasks.named<ShadowJar>("reobfShadowJar").flatMap { it.archiveFile })
        minecraftVersions.set(mojmapPaperVersions)
        modLoaders.set(listOf("spigot"))
    }

    // Mark reobfJar as spigot & paper for <1.20.5
    modrinth("spigotAndOldPaper") {
        from(common)
        file.set(tasks.named<ShadowJar>("reobfShadowJar").flatMap { it.archiveFile })
        minecraftVersions.set(spigotMappedPaperVersions)
        modLoaders.set(listOf("paper", "spigot"))
    }

    // Mark mojang mapped jar as paper 1.20.5+ only
    modrinth {
        from(common)
        file.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
        minecraftVersions.set(mojmapPaperVersions)
        modLoaders.set(listOf("paper"))
    }

    // dryRun.set(true) // For testing
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}
