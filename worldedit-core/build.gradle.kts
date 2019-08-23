import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java-library")
    id("net.ltgt.apt-eclipse")
    id("net.ltgt.apt-idea")
}

applyPlatformAndCoreConfiguration()

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:21.0")
    }
}

dependencies {
    "compile"(project(":worldedit-libs:core"))
    "compile"("de.schlichtherle:truezip:6.8.3")
    "compile"("rhino:js:1.7R2")
    "compile"("org.yaml:snakeyaml:1.23")
    "compile"("com.google.guava:guava:21.0")
    "compile"("com.google.code.findbugs:jsr305:3.0.2")
    "compile"("com.google.code.gson:gson:2.8.0")
    "compile"("org.slf4j:slf4j-api:1.7.26")
    "compile"("it.unimi.dsi:fastutil:8.2.1")
    "compile"("com.googlecode.json-simple:json-simple:1.1.1") { isTransitive = false }
    "compileOnly"(project(":worldedit-libs:core:ap"))
    "annotationProcessor"(project(":worldedit-libs:core:ap"))
    // ensure this is on the classpath for the AP
    "annotationProcessor"("com.google.guava:guava:21.0")
    "compileOnly"("com.google.auto.value:auto-value-annotations:${Versions.AUTO_VALUE}")
    "annotationProcessor"("com.google.auto.value:auto-value:${Versions.AUTO_VALUE}")
    "compile"("co.aikar:fastutil-lite:1.0")
    "compile"("com.github.luben:zstd-jni:1.4.3-1")
    //"compile"("com.mojang:datafixerupper:1.0.20")
    "compileOnly"("com.github.intellectualsites.plotsquared:PlotSquared-API:latest") {
        isTransitive = false
    }
    "compileOnly"("net.fabiozumbi12:redprotect:1.9.6")
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":worldedit-libs:build")
    options.compilerArgs.add("-Aarg.name.key.prefix=")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir("src/legacy/java")
        }
        resources {
            srcDir("src/main/resources")
        }
    }
}

tasks.named<Copy>("processResources") {
    filesMatching("fawe.properties") {
        expand("version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}")
    }
}
tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("com.github.luben:zstd-jni:1.4.3-1"))

    }
}
