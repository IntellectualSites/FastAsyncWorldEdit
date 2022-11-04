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
            version { strictly("1.33") }
            because("Bukkit provides SnakeYaml")
        }
    }

    // Modules
    api(projects.worldeditLibs.core)
    compileOnly(projects.worldeditLibs.core.ap)
    annotationProcessor(projects.worldeditLibs.core.ap)

    // Minecraft expectations
    implementation(libs.fastutil)
    implementation("com.google.guava:guava")
    implementation("com.google.code.gson:gson")

    implementation(libs.jchronic) {
        exclude(group = "junit", module = "junit")
    }
    implementation("com.thoughtworks.paranamer:paranamer:2.6")
    implementation(libs.jlibnoise)
    api(platform("org.enginehub.lin-bus:lin-bus-bom:0.1.0-SNAPSHOT"))
    api("org.enginehub.lin-bus:lin-bus-tree:0.1.0-SNAPSHOT") {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    api("org.enginehub.lin-bus.format:lin-bus-format-snbt:0.1.0-SNAPSHOT") {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // Platform expectations
    implementation("org.yaml:snakeyaml")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api")

    // Plugins
    compileOnly(libs.redprotect) { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Core") { isTransitive = false }

    // ensure this is on the classpath for the AP
    annotationProcessor(libs.guava)
    compileOnly(libs.autoValueAnnotations)
    annotationProcessor(libs.autoValue)

    // Third party
    compileOnly(libs.truezip)
    implementation(libs.findbugs)
    implementation(libs.rhino)
    compileOnly("net.kyori:adventure-api")
    compileOnlyApi(libs.adventureNbt)
    compileOnlyApi("net.kyori:adventure-text-minimessage")
    implementation(libs.zstd) { isTransitive = false }
    compileOnly("com.intellectualsites.paster:Paster")
    compileOnly(libs.lz4Java) { isTransitive = false }
    compileOnly(libs.sparsebitset)
    compileOnly(libs.parallelgzip) { isTransitive = false }
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
