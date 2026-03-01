import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("buildlogic.platform")
}

project.description = "Nukkit-MOT"

platform {
    kind = buildlogic.WorldEditKind.Plugin
    includeClasspath = true
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "OpenCollab Releases"
        url = uri("https://repo.opencollab.dev/maven-releases/")
    }
    maven {
        name = "OpenCollab Snapshots"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        name = "repo-lanink-cn"
        url = uri("https://repo.lanink.cn/repository/maven-public/")
    }
    ivy {
        url = uri("https://raw.githubusercontent.com/")
        patternLayout {
            artifact("[organisation]/[revision]/[artifact].([ext])")
            setM2compatible(true)
        }
        metadataSources {
            artifact()
        }
    }
}

val geyserMappings: Configuration by configurations.register("geyserMappings") {
    isCanBeConsumed = false
}

val mcmeta: Configuration by configurations.register("mcmeta") {
    isCanBeConsumed = false
}

dependencies {
    api(project(":worldedit-core"))
    api(project(":worldedit-libs:nukkit-mot"))

    compileOnly("cn.nukkit:Nukkit:MOT-SNAPSHOT")

    implementation(libs.fastutil)
    implementation(libs.gson)

    api(libs.lz4Java) { isTransitive = false }
    api(libs.sparsebitset) { isTransitive = false }
    api(libs.parallelgzip) { isTransitive = false }
    compileOnlyApi(libs.checkerqual)

    geyserMappings("GeyserMC.mappings", "items", "15398c1", ext = "json")
    geyserMappings("GeyserMC.mappings", "biomes", "15398c1", ext = "json")
    geyserMappings("GeyserMC.mappings-generator", "generator_blocks", "8fa6058", ext = "json")
    mcmeta("misode.mcmeta", "blocks/data", "cb195b9", ext = "json")
}

tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("internalVersion" to internalVersion))
    }
    from(geyserMappings) {
        into("mapping")
        rename("(?:generator_)?([^-]+)-(.*)\\.json", "$1.json")
    }
    from(mcmeta) {
        rename("data-(.*)\\.json", "je_blocks.json")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName.set("${rootProject.name}-Nukkit-MOT-${project.version}.${archiveExtension.getOrElse("jar")}")
    dependencies {
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:nukkit-mot"))
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4") {
            include(dependency("org.antlr:antlr4-runtime"))
        }
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency(libs.fastutil))
        }
        relocate("net.royawesome.jlibnoise", "com.sk89q.worldedit.jlibnoise") {
            include(dependency("com.sk89q.lib:jlibnoise"))
        }
        relocate("com.zaxxer", "com.fastasyncworldedit.core.math") {
            include(dependency(libs.sparsebitset))
        }
        relocate("org.anarres", "com.fastasyncworldedit.core.internal.io") {
            include(dependency(libs.parallelgzip))
        }
        // ZSTD does not work if relocated. https://github.com/luben/zstd-jni/issues/189
        include(dependency(libs.zstd))
        include(dependency(libs.lz4Java))
    }
    minimize {
        exclude(dependency(libs.lz4Java))
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        from(components["java"])
    }
}
