import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.4"
    id("com.gradleup.shadow") version "9.5.1"
}

group = "com.fastasyncworldedit"
version = "0.0.1-M0"

val faweCoreVersion = "2.15.5-SNAPSHOT"
val libsPrefix = "com.fastasyncworldedit.forge1710.libs"

base {
    archivesName.set("FastAsyncWorldEdit-Forge1710")
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")
}

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("com\\.fastasyncworldedit(\\..+)?")
        }
    }
    maven {
        name = "EngineHub Repository"
        url = uri("https://maven.enginehub.org/repo/")
        content {
            excludeGroup("net.kyori")
        }
    }
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    mavenCentral()
}

// Everything in here is bundled into the mod jar (and relocated where it could clash with the 1.7.10 classpath).
// Exclusions live on the shadowJar task, not here: configuration-level excludes are inherited by compileOnly and would
// also strip Forge's own log4j from the compile classpath.
val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.compileOnly.get().extendsFrom(shade)

// FAWE core pulls in log4j-api 2.17, but Minecraft 1.7.10 provides 2.0-beta9 at runtime. Compile against the runtime
// API so Logger calls bind to overloads that exist (core itself is built with -Pfawe.log4jApiVersion=2.0-beta9).
configurations.configureEach {
    resolutionStrategy.force("org.apache.logging.log4j:log4j-api:2.0-beta9")
}

dependencies {
    shade("com.fastasyncworldedit:FastAsyncWorldEdit-Core:$faweCoreVersion")
    // Provided by Paper on Bukkit; FAWE core only declares them compileOnly, so bundle them here.
    shade("net.kyori:adventure-api:5.2.0")
    shade("net.kyori:adventure-text-minimessage:5.2.0")
    shade("at.yawk.lz4:lz4-java:1.11.3") { isTransitive = false }
    shade("com.zaxxer:SparseBitSet:1.3") { isTransitive = false }
    shade("org.anarres:parallelgzip:1.0.5") { isTransitive = false }
    shade("com.intellectualsites.paster:Paster:1.1.7") { isTransitive = false }

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
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

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-all")
    configurations = listOf(shade)
    mergeServiceFiles()
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    exclude("META-INF/maven/**", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    dependencies {
        // Provided by Forge (log4j) or annotation-only artifacts that are not needed at runtime.
        exclude(dependency("org.apache.logging.log4j:.*:.*"))
        exclude(dependency("com.google.code.findbugs:jsr305:.*"))
        exclude(dependency("org.checkerframework:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("com.google.errorprone:.*:.*"))
        exclude(dependency("com.google.j2objc:.*:.*"))
        // Gradle's antlr plugin leaks the ANTLR *tool* into core's published runtime dependencies. Only
        // antlr4-runtime is needed; icu4j in particular would clash with Forge's icu4j-core-mojang (com.ibm.icu).
        exclude(dependency("org.antlr:antlr4:.*"))
        exclude(dependency("org.antlr:antlr-runtime:.*"))
        exclude(dependency("org.antlr:ST4:.*"))
        exclude(dependency("org.abego.treelayout:.*:.*"))
        exclude(dependency("com.ibm.icu:.*:.*"))
        exclude(dependency("org.glassfish:javax.json:.*"))
    }

    // Same targets as worldedit-bukkit so FAWE-internal reflection keeps working.
    relocate("com.sk89q.jchronic", "com.sk89q.worldedit.jchronic")
    relocate("org.antlr.v4", "com.sk89q.worldedit.antlr4")
    relocate("net.royawesome.jlibnoise", "com.sk89q.worldedit.jlibnoise")
    relocate("com.intellectualsites.paster", "com.fastasyncworldedit.paster")
    relocate("com.zaxxer", "com.fastasyncworldedit.core.math")
    relocate("org.anarres", "com.fastasyncworldedit.core.internal.io")

    // Libraries Paper provides but 1.7.10 either lacks or ships an ancient copy of (Guava 17/20, Gson, SnakeYAML).
    relocate("com.google.common", "$libsPrefix.guava")
    relocate("com.google.thirdparty", "$libsPrefix.guava.thirdparty")
    relocate("com.google.gson", "$libsPrefix.gson")
    relocate("it.unimi.dsi.fastutil", "$libsPrefix.fastutil")
    relocate("org.yaml.snakeyaml", "$libsPrefix.snakeyaml")
    relocate("org.mozilla", "$libsPrefix.mozilla")
    relocate("org.json.simple", "$libsPrefix.jsonsimple")
    relocate("net.kyori.adventure", "$libsPrefix.adventure")
    relocate("net.kyori.examination", "$libsPrefix.examination")
    relocate("net.kyori.option", "$libsPrefix.option")
    // zstd-jni must not be relocated (its natives look up the original class names); lz4-java is left as-is too.
}

tasks.named<ReobfuscatedJar>("reobfJar") {
    inputJar.set(shadowJar.flatMap { it.archiveFile })
}
