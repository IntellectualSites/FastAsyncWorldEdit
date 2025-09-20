import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`

    alias(libs.plugins.pluginyml)
}

project.description = "CLI"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

addJarManifest(
        WorldEditKind.Standalone("com.sk89q.worldedit.cli.CLIWorldEdit"),
        extraAttributes = mapOf(
                // We don't have any multi-release stuff, but Log4J does.
                "Multi-Release" to "true",
        ),
)

dependencies {
    // Modules
    api(projects.worldeditCore)
    compileOnly(projects.worldeditLibs.core.ap)
    annotationProcessor(projects.worldeditLibs.core.ap)

    // Minecraft expectations
    annotationProcessor(libs.guava)
    implementation(libs.guava)
    implementation(libs.gson)

    // Logging
    implementation(libs.log4jBom) {
        because("We control Log4J on this platform")
    }
    implementation(libs.log4jApi)
    implementation(libs.log4jCore)
    implementation(libs.commonsCli)
    api(libs.parallelgzip) { isTransitive = false }
    api(libs.lz4Java)
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include { true }
        relocate("org.anarres", "com.fastasyncworldedit.core.internal.io")
        relocate("net.jpountz", "com.fastasyncworldedit.core.jpountz")
        relocate("org.lz4", "com.fastasyncworldedit.core.lz4")
    }
    archiveFileName.set(moduleIdentifier)
    minimize {
        exclude(dependency("org.apache.logging.log4j:log4j-core"))
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

val moduleIdentifier = "${rootProject.name}-${project.description}-${project.version}.jar"
val decoration = "\n****************************************"
val websiteURL = "https://modrinth.com/plugin/fastasyncworldedit/"

bukkit {
    name = "FastAsyncWorldEdit-COMMAND_LINE_INTERFACE_NOT_A_PLUGIN"
    main = "com.sk89q.worldedit.cli.AccessPoint"
    apiVersion = decoration +
            "\n* 404 - Plugin Not Found.\n" +
            "* You installed the command line interface (CLI) which is not a plugin.\n" +
            "* Stop your server, delete `$moduleIdentifier`" +
            " and download the proper one from:\n" +
            "* $websiteURL\n" +
            "* (contains `-Paper-` instead of `-CLI-` in the name ;)" +
            decoration
    version = rootProject.version.toString()
    website = websiteURL
}
