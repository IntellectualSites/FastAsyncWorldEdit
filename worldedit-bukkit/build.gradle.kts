import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven {
        name = "Spigot"
        url = uri("https://hub.spigotmc.org/nexus/content/groups/public")
        content {
            includeGroup("org.bukkit")
            includeGroup("org.spigotmc")
        }
    }
    maven {
        name = "Paper"
        url = uri("https://papermc.io/repo/repository/maven-public/")
        content {
            includeGroup("io.papermc")
            includeGroup("com.destroystokyo.paper")
        }
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            includeGroupByRegex("com.sk89q.*")
        }
    }
    maven {
        name = "Athion"
        url = uri("https://ci.athion.net/plugin/repository/tools/")
    }
    maven {
        this.name = "JitPack"
        this.url = uri("https://jitpack.io")
        content {
            includeGroup("net.milkbowl.vault")
            includeGroup("com.github.TechFortress")
        }
    }
    maven {
        name = "ProtocolLib Repo"
        url = uri("https://repo.dmulloy2.net/nexus/repository/public/")
        content {
            includeGroup("com.comphenix.protocol")
        }
    }
    maven {
        name = "Inventivetalent"
        url = uri("https://repo.inventivetalent.org/content/groups/public/")
        content {
            includeGroupByRegex("org.inventivetalent.*")
        }
    }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.org/repository/maven-public/")
        content {
            includeGroup("org.bstats")
        }
    }
    flatDir { dir(File("src/main/resources")) }
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:21.0")
    }
}

dependencies {
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    api(project(":worldedit-core"))
    api(project(":worldedit-libs:bukkit"))
    compile(":worldedit-adapters:")
    // Paper-patched NMS jars
    compileOnly("com.destroystokyo.paperv1_15_r1:paperv1_15_r1:1_15_r1")
    compileOnly("com.destroystokyo.paperv1_16_r1:paperv1_16_r1:1_16_r1")
    compileOnly("com.destroystokyo.paperv1_16_r2:paperv1_16_r2:1_16_r2")
    compileOnly("com.destroystokyo.paperv1_16_r3:paperv1_16_r3:1_16_r3")
    compileOnly("org.spigotmcv1_15_r1:spigotmcv1_15_r1:1_15_r1")
    compileOnly("org.spigotmcv1_16_r1:spigotmcv1_16_r1:1_16_r1")
    compileOnly("org.spigotmcv1_16_r2:spigotmcv1_16_r2:1_16_r2")
    compileOnly("org.spigotmcv1_16_r3:spigotmcv1_16_r3:1_16_r3")
    implementation("it.unimi.dsi:fastutil:8.2.1")
    api("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
        isTransitive = false
    }
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:20.1.0")
    testCompileOnly("org.jetbrains:annotations:20.1.0")
    implementation("io.papermc:paperlib:1.0.6")
    compileOnly("com.sk89q:dummypermscompat:1.10") {
        exclude("com.github.MilkBowl", "VaultAPI")
    }
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1")
    testImplementation("org.mockito:mockito-core:1.9.0-rc1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4") {
        exclude("com.sk89q.worldedit", "worldedit-bukkit")
        exclude("com.sk89q.worldedit", "worldedit-core")
        exclude("com.sk89q.worldedit.worldedit-libs", "bukkit")
        exclude("com.sk89q.worldedit.worldedit-libs", "core")
    }
    api("com.intellectualsites.paster:Paster:1.0.1-SNAPSHOT")
    // Third party
    implementation("org.bstats:bstats-bukkit:1.8")
    compileOnlyApi("org.inventivetalent:mapmanager:1.7.+") { isTransitive = false }
    implementation("com.github.TechFortress:GriefPrevention:16.+") { isTransitive = false }
    implementation("com.massivecraft:mcore:7.0.1") { isTransitive = false }
    implementation("com.bekvon.bukkit.residence:Residence:4.5._13.1") { isTransitive = false }
    implementation("com.palmergames.bukkit:towny:0.84.0.9") { isTransitive = false }
    implementation("com.thevoxelbox.voxelsniper:voxelsniper:5.171.0") { isTransitive = false }
    implementation("com.comphenix.protocol:ProtocolLib:4.5.1") { isTransitive = false }
}

tasks.named<Copy>("processResources") {
    filesMatching("plugin.yml") {
        expand("internalVersion" to project.ext["internalVersion"])
    }
    // exclude adapters entirely from this JAR, they should only be in the shadow JAR
    exclude("**/worldedit-adapters.jar")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    from(zipTree("src/main/resources/worldedit-adapters.jar").matching {
        exclude("META-INF/")
    })
    dependencies {
        relocate("org.slf4j", "com.sk89q.worldedit.slf4j")
        relocate("org.apache.logging.slf4j", "com.sk89q.worldedit.log4jbridge")
        relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
        include(dependency(":worldedit-core"))
        include(dependency(":worldedit-libs:bukkit"))
        include(dependency("org.slf4j:slf4j-api"))
        include(dependency("org.apache.logging.log4j:log4j-slf4j-impl"))
        include(dependency("org.antlr:antlr4-runtime"))
        relocate("io.papermc.lib", "com.sk89q.worldedit.bukkit.paperlib") {
            include(dependency("io.papermc:paperlib:1.0.6"))
        }
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.bukkit.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
        relocate("org.bstats", "com.boydti.metrics") {
            include(dependency("org.bstats:bstats-bukkit:1.8"))
        }
        relocate("com.intellectualsites.paster", "com.boydti.fawe.paster") {
            include(dependency("com.intellectualsites.paster:Paster:1.0.1-SNAPSHOT"))
        }
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
