import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    `java-library`
    antlr
}

project.description = "Core"

applyPlatformAndCoreConfiguration()

dependencies {
    constraints {
        implementation("org.yaml:snakeyaml") {
            version { strictly("1.28") }
            because("Bukkit provides SnakeYaml")
        }
    }

    // Modules
    api(projects.worldeditLibs.core)
    compileOnly(projects.worldeditLibs.core.ap)
    annotationProcessor(projects.worldeditLibs.core.ap)

    // Minecraft expectations
    implementation(libs.fastutil)
    implementation(libs.guava)
    implementation(libs.gson)

    // Platform expectations
    implementation("org.yaml:snakeyaml")

    // Logging
    implementation(libs.log4j) {
        because("Mojang provides Log4J 2.14.1")
    }

    // Plugins
    compileOnly(libs.redprotect)
    api(libs.plotsquaredV4) { isTransitive = false }
    api(libs.plotsquaredV6Core) { isTransitive = false }

    // ensure this is on the classpath for the AP
    annotationProcessor(libs.guava)
    compileOnly(libs.autoValueAnnotations)
    annotationProcessor(libs.autoValue)

    // Third party
    implementation(libs.truezip)
    implementation(libs.findbugs)
    implementation(libs.rhino)
    compileOnly(libs.adventure)
    compileOnlyApi(libs.adventureNbt)
    compileOnlyApi(libs.adventureTextMiniMessage)
    implementation(libs.zstd)
    api(libs.paster)
    compileOnly(libs.lz4Java) { isTransitive = false }
    compileOnly(libs.lz4JavaStream)
    compileOnly(libs.sparsebitset)
    compileOnly(libs.parallelgzip)
    antlr(libs.antlr4)
    implementation(libs.antlr4Runtime)
    implementation(libs.jsonSimple) { isTransitive = false }

    // Tests
    testRuntimeOnly(libs.log4jCore)
    testImplementation(libs.adventureNbt)
    testImplementation(libs.parallelgzip)
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
