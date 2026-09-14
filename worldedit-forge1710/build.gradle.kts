plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.4"
}

group = "com.fastasyncworldedit"
version = "0.0.1-S0"

base {
    archivesName.set("FastAsyncWorldEdit-Forge1710")
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")
}

repositories {
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    mavenCentral()
}

// The runtime is Java 25 (lwjgl3ify / RetroFuturaBootstrap); FAWE core needs Java 21 (virtual threads).
// Emit classfile 65 so the jar also works on any Java 21+ launcher.
// Only our own source sets: RFG's compilePatchedMcJava must stay on Java 8 (Forge uses java.util.jar.Pack200).
tasks.withType<JavaCompile>().matching { it.name == "compileJava" || it.name == "compileTestJava" }.configureEach {
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}
