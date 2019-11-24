import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("http://empcraft.com/maven2") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("http://ci.frostcast.net/plugin/repository/everything") }
    maven { url = uri("http://dl.bintray.com/tastybento/maven-repo") }
    maven { url = uri("http://ci.emc.gs/nexus/content/groups/aikar/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.destroystokyo.com/repository/maven-public//") }
    maven { url = uri("http://repo.dmulloy2.net/content/groups/public/") }
    maven { url = uri("http://ci.ender.zone/plugin/repository/everything/") }
    maven { url = uri("https://repo.inventivetalent.org/content/groups/public/")}
    flatDir {dir(File("src/main/resources"))}
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:21.0")
    }
}

dependencies {
    "compile"("net.milkbowl.vault:VaultAPI:1.7") { isTransitive = false }
    "api"(project(":worldedit-core"))
    "api"(project(":worldedit-libs:core"))
    "api"(project(":worldedit-libs:bukkit"))
    "compile"(":worldedit-adapters:")
    "compile"("org.spigotmcv1_13_r2:spigotmcv1_13_r2:1_13_r2")
    "compile"("it.unimi.dsi:fastutil:8.2.1")
    "api"("com.destroystokyo.paper:paper-api:1.14.4-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
        isTransitive = false
    }
    "compileOnly"("org.spigotmc:spigot:1.14.4-R0.1-SNAPSHOT")
    "implementation"("io.papermc:paperlib:1.0.2")
    "compileOnly"("com.sk89q:dummypermscompat:1.10")
    "implementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1")
    "testCompile"("org.mockito:mockito-core:1.9.0-rc1")
    "compileOnly"("com.sk89q.worldguard:worldguard-bukkit:7.0.0") {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldedit.worldedit-libs", "bukkit")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    "implementation"("org.inventivetalent:mapmanager:1.7.3-SNAPSHOT") { isTransitive = false }
    "implementation"("org.inventivetalent:mapmanager:1.7.3-SNAPSHOT") { isTransitive = false }

    "implementation"("com.massivecraft:factions:2.8.0") { isTransitive = false }
    "implementation"("com.drtshock:factions:1.6.9.5") { isTransitive = false }
    "implementation"("com.github.TechFortress:GriefPrevention:16.12.0") { isTransitive = false }
    "implementation"("com.massivecraft:mcore:7.0.1") { isTransitive = false }
    "implementation"("net.jzx7:regios:5.9.9") { isTransitive = false }
    "implementation"("com.bekvon.bukkit.residence:Residence:4.5._13.1") { isTransitive = false }
    "implementation"("com.palmergames.bukkit:towny:0.84.0.9") { isTransitive = false }
    "implementation"("com.thevoxelbox.voxelsniper:voxelsniper:5.171.0") { isTransitive = false }
    "implementation"("com.comphenix.protocol:ProtocolLib-API:4.4.0-SNAPSHOT") { isTransitive = false }
    "implementation"("com.wasteofplastic:askyblock:3.0.8.2") { isTransitive = false }
}

tasks.named<Copy>("processResources") {
    filesMatching("plugin.yml") {
        expand("internalVersion" to project.ext["internalVersion"])
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to "truezip.jar WorldEdit/truezip.jar js.jar WorldEdit/js.jar",
                "WorldEdit-Version" to project.version)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        relocate("org.slf4j", "com.sk89q.worldedit.slf4j")
        relocate("org.apache.logging.slf4j", "com.sk89q.worldedit.log4jbridge")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:core"))
        include(dependency(":worldedit-libs:bukkit"))
        include(dependency("org.slf4j:slf4j-api"))
        include(dependency("org.apache.logging.log4j:log4j-slf4j-impl"))
        include(dependency("org.antlr:antlr4-runtime"))
        relocate("org.bstats", "com.sk89q.worldedit.bukkit.bstats") {
            include(dependency("org.bstats:bstats-bukkit:1.5"))
        }
        relocate("io.papermc.lib", "com.sk89q.worldedit.bukkit.paperlib") {
            include(dependency("io.papermc:paperlib:1.0.2"))
        }
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
