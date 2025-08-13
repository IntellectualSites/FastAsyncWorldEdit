import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.plugins.antlr.AntlrTask

plugins {
    `java-library`
    antlr
}

project.description = "Core"

applyPlatformAndCoreConfiguration()

dependencies {
    constraints {
        implementation(libs.snakeyaml) {
            version { strictly("2.2") }
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
    implementation(libs.snakeyaml)

    // Logging
    implementation(libs.log4j.api)

    // Plugins
    compileOnly(libs.plotsquared.core) { isTransitive = false }

    // ensure this is on the classpath for the AP
    annotationProcessor(libs.guava)
    compileOnly(libs.autoValueAnnotations)
    annotationProcessor(libs.autoValue)

    // Third party
    compileOnly(libs.truezip)
    implementation(libs.findbugs)
    implementation(libs.rhino)
    compileOnly(libs.adventureApi)
    compileOnlyApi(libs.adventureMiniMessage)
    implementation(libs.zstd) { isTransitive = false }
    compileOnly(libs.paster)
    compileOnly(libs.lz4Java) { isTransitive = false }
    compileOnly(libs.sparsebitset)
    compileOnly(libs.parallelgzip) { isTransitive = false }
    antlr(libs.antlr4)
    implementation(libs.antlr4.runtime)
    implementation(libs.json.simple) { isTransitive = false }
    implementation(platform(libs.linBus.bom))

    // Tests
    testRuntimeOnly(libs.log4j.core)
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

tasks.named<Jar>("sourcesJar") {
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
        expand(
                "version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}"
        )
    }
}
