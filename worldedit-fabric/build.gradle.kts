import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
}
loom {
    serverOnlyMinecraftJar()

    splitEnvironmentSourceSets()
    mods {
        register("fawe") {
            sourceSet(sourceSets["main"])
                    sourceSet(sourceSets["client"])
        }
    }
}
repositories {

}
dependencies {
    implementation(project(":worldedit-core"));

    minecraft("com.mojang:minecraft:${project.findProperty("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${project.findProperty("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.findProperty("fabric_api_version")}")
}
tasks.processResources {
    inputs.property("version", project.rootProject.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.rootProject.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    inputs.property("archivesName", project.base.archivesName)

    from("LICENSE") {
        rename { "${it}_${inputs.properties["archivesName"]}" }
    }
}
