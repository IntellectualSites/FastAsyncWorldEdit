import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    `java-library`
    antlr
}

repositories {
    maven {
        name = "IntellectualSites"
        url = uri("https://mvn.intellectualsites.com/content/groups/public/")
    }
}

applyPlatformAndCoreConfiguration()

dependencies {
    constraints {
        implementation( "org.yaml:snakeyaml") {
            version { strictly("1.27") }
            because("Bukkit provides SnakeYaml")
        }
    }

    api(project(":worldedit-libs:core"))
    implementation("de.schlichtherle:truezip:6.8.4")
    implementation("org.mozilla:rhino-runtime:1.7.13")
    implementation("org.yaml:snakeyaml")
    implementation("com.google.guava:guava")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.code.gson:gson")
    implementation("org.apache.logging.log4j:log4j-api:2.8.1") {
        because("Mojang provides Log4J 2.8.1")
    }
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
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:2.8.1")
    implementation("com.github.luben:zstd-jni:1.5.0-2")
    compileOnly("net.fabiozumbi12:redprotect:1.9.6")
    api("com.github.intellectualsites.plotsquared:PlotSquared-API:4.514") { isTransitive = false }
    api("com.plotsquared:PlotSquared-Core:5.13.11") { isTransitive = false }
    api("com.intellectualsites.paster:Paster:1.0.1-SNAPSHOT")
    compileOnly("net.jpountz:lz4-java-stream:1.0.0") { isTransitive = false }
    compileOnly("org.lz4:lz4-java:1.7.1")
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

tasks.named("sourcesJar") {
    mustRunAfter("generateGrammarSource")
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
        srcDir("src/legacy/java")
    }
}

tasks.named<Copy>("processResources") {
    filesMatching("fawe.properties") {
        expand("version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}")
    }
}
