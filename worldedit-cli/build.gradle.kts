import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("net.minecrell.plugin-yml.bukkit") version "0.4.0"
}

project.description = "CLI"

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

addJarManifest(WorldEditKind.Standalone("com.sk89q.worldedit.cli.CLIWorldEdit"))

dependencies {
    compileOnly(project(":worldedit-libs:core:ap"))
    annotationProcessor(project(":worldedit-libs:core:ap"))
    annotationProcessor("com.google.guava:guava:${Versions.GUAVA}")
    api(project(":worldedit-core"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.14.1") {
        because("We control Log4J on this platform")
    })
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("commons-cli:commons-cli:1.4")
    implementation("com.google.guava:guava")
    implementation("com.google.code.gson:gson")
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include { true }
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
val websiteURL = "https://www.spigotmc.org/resources/13932/"

bukkit {
    name = "FastAsyncWorldEdit-COMMAND_LINE_INTERFACE_NOT_A_PLUGIN"
    main = "com.sk89q.worldedit.cli.AccessPoint"
    apiVersion = decoration +
            "\n* 404 - Plugin Not Found.\n" +
            "* You installed the command line interface (CLI) which is not a plugin.\n" +
            "* Stop your server, delete `$moduleIdentifier`" +
            " and download the proper one from:\n" +
            "* $websiteURL\n" +
            "* (contains `-Bukkit-` instead of `-CLI-` in the name ;)" +
            decoration
    version = rootProject.version.toString()
    website = websiteURL
}
