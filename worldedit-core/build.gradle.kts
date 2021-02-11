import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("java-library")
    id("net.ltgt.apt-eclipse")
    id("net.ltgt.apt-idea")
    id("antlr")
}

repositories {
    maven {
        name = "IntellectualSites"
        url = uri("https://mvn.intellectualsites.com/content/groups/public/")
        content {
            includeGroup("com.plotsquared")
            includeGroup("com.intellectualsites.paster")
            includeGroup("com.github.intellectualsites.plotsquared")
        }
    }
}

applyPlatformAndCoreConfiguration()

dependencies {
    constraints {
        implementation( "org.yaml:snakeyaml") {
            version { strictly("1.26") }
            because("Bukkit provides SnakeYaml")
        }
    }

    api(project(":worldedit-libs:core"))
    implementation("de.schlichtherle:truezip:6.8.4")
    implementation("net.java.truevfs:truevfs-profile-default_2.13:0.12.1")
    implementation("org.mozilla:rhino-runtime:1.7.13")
    implementation("org.yaml:snakeyaml")
    implementation("com.google.guava:guava")
    implementation("com.google.code.findbugs:jsr305:1.3.9")
    implementation("com.google.code.gson:gson")
    implementation("org.slf4j:slf4j-api:${Versions.SLF4J}")
    implementation("it.unimi.dsi:fastutil")

    val antlrVersion = "4.9.1"
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")

    implementation("com.googlecode.json-simple:json-simple:1.1.1") { isTransitive = false }
    compileOnly(project(":worldedit-libs:core:ap"))
    annotationProcessor(project(":worldedit-libs:core:ap"))
    // ensure this is on the classpath for the AP
    annotationProcessor("com.google.guava:guava:21.0")
    compileOnly("com.google.auto.value:auto-value-annotations:${Versions.AUTO_VALUE}")
    annotationProcessor("com.google.auto.value:auto-value:${Versions.AUTO_VALUE}")
    testImplementation("ch.qos.logback:logback-core:${Versions.LOGBACK}")
    testImplementation("ch.qos.logback:logback-classic:${Versions.LOGBACK}")
    implementation("com.github.luben:zstd-jni:1.4.8-2")
    compileOnly("net.fabiozumbi12:redprotect:1.9.6")
    api("com.github.intellectualsites.plotsquared:PlotSquared-API:4.514") { isTransitive = false }
    api("com.plotsquared:PlotSquared-Core:5.13.3") { isTransitive = false }
    api("com.intellectualsites.paster:Paster:1.0.1-SNAPSHOT")
}

tasks.named<Test>("test") {
    maxHeapSize = "1G"
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":worldedit-libs:build")
    options.compilerArgs.add("-Aarg.name.key.prefix=")
}

tasks.named<AntlrTask>("generateGrammarSource").configure {
    val pkg = "com.sk89q.worldedit.antlr"
    outputDirectory = file("build/generated-src/antlr/main/${pkg.replace('.', '/')}")
    arguments = listOf(
            "-visitor", "-package", pkg,
            "-Xexact-output-dir"
    )
}

// Give intellij info about where ANTLR code comes from
plugins.withId("idea") {
    configure<IdeaModel> {
        afterEvaluate {
            module.sourceDirs.add(file("src/main/antlr"))
            module.sourceDirs.add(file("build/generated-src/antlr/main"))
            module.generatedSourceDirs.add(file("build/generated-src/antlr/main"))
        }
    }
}

sourceSets.named("main") {
    java {
        srcDir("src/main/java")
        srcDir("src/legacy/java")
    }
    resources {
        srcDir("src/main/resources")
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
        include(dependency("com.github.luben:zstd-jni:1.4.8-2"))

    }
}
