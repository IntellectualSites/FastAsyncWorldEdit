import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.spongepowered.plugin")
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    maven { url = uri("https://repo.codemc.org/repository/maven-public") }
}

dependencies {
    compile(project(":worldedit-core"))
    compile(project(":worldedit-libs:sponge"))
    compile("org.spongepowered:spongeapi:7.1.0")
    compile("org.bstats:bstats-sponge:1.5")
    testCompile("org.mockito:mockito-core:3.7.0")
}

sponge {
    plugin {
        id = "worldedit"
    }
}

<<<<<<< HEAD
addJarManifest(includeClasspath = true)
=======
tasks.named<Jar>("jar") {
    manifest {
        attributes("Class-Path" to CLASSPATH,
                "WorldEdit-Version" to project.version)
    }
}
>>>>>>> 18a55bc14... Add new experimental snapshot API (#524)

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        relocate ("org.bstats", "com.sk89q.worldedit.sponge.bstats") {
            include(dependency("org.bstats:bstats-sponge:1.5"))
        }
    }
}

if (project.hasProperty("signing")) {
    apply(plugin = "signing")

    configure<SigningExtension> {
        sign("shadowJar")
    }

    tasks.named("build").configure {
        dependsOn("signShadowJar")
    }
}
