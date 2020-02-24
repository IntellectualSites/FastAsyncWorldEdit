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
    maven { url = uri("http://ci.emc.gs/nexus/content/groups/aikar/") }
    maven {
        this.name = "JitPack"
        this.url = uri("https://jitpack.io")
    }
    maven { url = uri("https://repo.destroystokyo.com/repository/maven-public/") }
    maven {
        name = "ProtocolLib Repo"
        url = uri("https://repo.dmulloy2.net/nexus/repository/public/")
    }
    maven { url = uri("https://repo.inventivetalent.org/content/groups/public/")}
    flatDir {dir(File("src/main/resources"))}
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:21.0")
    }
}

dependencies {
    compile("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    "api"(project(":worldedit-core"))
    "api"(project(":worldedit-libs:bukkit"))
    "compile"(":worldedit-adapters:")
    "compile"("org.spigotmcv1_14_r1:spigotmcv1_14_r1:1_14_r1")
    "compile"("org.spigotmcv1_15_r1:spigotmcv1_15_r1:1_15_r1")
    "compile"("it.unimi.dsi:fastutil:8.2.1")
    "api"("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
        isTransitive = false
    }
    "compileOnly"("org.spigotmc:spigot:1.14.4-R0.1-SNAPSHOT")
    "compileOnly"("org.spigotmc:spigot:1.15.2-R0.1-SNAPSHOT")
    "implementation"("io.papermc:paperlib:1.0.2")
    "compileOnly"("com.sk89q:dummypermscompat:1.10")
    "implementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1")
    "testCompile"("org.mockito:mockito-core:1.9.0-rc1")
    "compileOnly"("com.sk89q.worldguard:worldguard-bukkit:7.+") {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldedit.worldedit-libs", "bukkit")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    "implementation"("org.inventivetalent:mapmanager:1.7.3-SNAPSHOT") { isTransitive = false }

    "implementation"("com.github.TechFortress:GriefPrevention:16.+") { isTransitive = false }
    "implementation"("com.massivecraft:mcore:7.0.1") { isTransitive = false }
    "implementation"("com.bekvon.bukkit.residence:Residence:4.5._13.1") { isTransitive = false }
    "implementation"("com.palmergames.bukkit:towny:0.84.0.9") { isTransitive = false }
    "implementation"("com.thevoxelbox.voxelsniper:voxelsniper:5.171.0") { isTransitive = false }
    "implementation"("com.comphenix.protocol:ProtocolLib:4.5.0") { isTransitive = false }
}

tasks.named<Copy>("processResources") {
    filesMatching("plugin.yml") {
        expand("internalVersion" to project.ext["internalVersion"])
    }
    from(zipTree("src/main/resources/worldedit-adapters.jar").matching {
        exclude("META-INF/")
    })
    exclude("**/worldedit-adapters.jar")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        relocate("org.slf4j", "com.sk89q.worldedit.slf4j")
        relocate("org.apache.logging.slf4j", "com.sk89q.worldedit.log4jbridge")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:bukkit"))
        include(dependency("org.slf4j:slf4j-api"))
        include(dependency("org.apache.logging.log4j:log4j-slf4j-impl"))
        include(dependency("org.antlr:antlr4-runtime"))
        relocate("org.bstats", "com.sk89q.worldedit.bukkit.bstats") {
            include(dependency("org.bstats:bstats-bukkit:1.7"))
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
