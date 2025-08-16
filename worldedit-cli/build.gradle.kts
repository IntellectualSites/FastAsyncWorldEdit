import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("buildlogic.platform")
    alias(libs.plugins.pluginyml)
}

platform {
    kind = buildlogic.WorldEditKind.Standalone("com.sk89q.worldedit.cli.CLIWorldEdit")
    extraAttributes = mapOf(
        // We don't have any multi-release stuff, but Log4J does.
        "Multi-Release" to "true",
    )
}

dependencies {
    "compileOnly"(project(":worldedit-libs:core:ap"))
    "annotationProcessor"(project(":worldedit-libs:core:ap"))
    "annotationProcessor"(libs.guava)
    "api"(project(":worldedit-core"))
    "implementation"(platform(libs.log4j.bom)) {
        because("We control Log4J on this platform")
    }
    "implementation"(libs.log4j.api)
    "implementation"(libs.log4j.core)
    "implementation"(libs.commonsCli)
    "implementation"(libs.guava)
    "implementation"(libs.gson)
    "api"(libs.parallelgzip) { isTransitive = false }
    "api"(libs.lz4Java)
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include { true }
        relocate("org.anarres", "com.fastasyncworldedit.core.internal.io")
        relocate("net.jpountz", "com.fastasyncworldedit.core.jpountz")
        relocate("org.lz4", "com.fastasyncworldedit.core.lz4")
    }
    //TODO Upstream doesn't include the line below so we should see if we need it or not.
    archiveFileName.set(moduleIdentifier)
    minimize {
        exclude(dependency("org.apache.logging.log4j:log4j-core"))
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        artifactId = the<BasePluginExtension>().archivesName.get()
        from(components["java"])
    }
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
