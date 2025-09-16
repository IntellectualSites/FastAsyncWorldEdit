import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    `java-library`
    antlr
    id("buildlogic.core-and-platform")
}

dependencies {
    constraints {
        implementation(libs.snakeyaml) {
            version { strictly("2.2") }
            because("Bukkit provides SnakeYaml")
        }
    }

    api(project(":worldedit-libs:core"))
    compileOnly(libs.trueZip)
    implementation(libs.rhino)
    implementation(libs.snakeyaml)
    implementation(libs.guava)
    compileOnlyApi(libs.jsr305)
    implementation(libs.gson)

    implementation(libs.jchronic) {
        exclude(group = "junit", module = "junit")
    }
    implementation(libs.jlibnoise)

    implementation(libs.log4j.api)

    compileOnly(libs.plotsquared.core) { isTransitive = false }

    implementation(libs.fastutil)

    antlr(libs.antlr4)
    implementation(libs.antlr4.runtime)

    compileOnly(project(":worldedit-libs:core:ap"))
    annotationProcessor(project(":worldedit-libs:core:ap"))
    // ensure this is on the classpath for the AP
    annotationProcessor(libs.guava)
    compileOnly(libs.autoValue.annotations)
    annotationProcessor(libs.autoValue)

    compileOnly(libs.autoService) {
        because("Needed to resolve annotations in Piston")
    }


    compileOnly(libs.adventureApi)
    compileOnlyApi(libs.adventureMiniMessage)
    implementation(libs.zstd) { isTransitive = false }
    compileOnly(libs.paster)
    compileOnly(libs.lz4Java) { isTransitive = false }
    compileOnly(libs.sparsebitset)
    compileOnly(libs.parallelgzip) { isTransitive = false }
    implementation(libs.json.simple) { isTransitive = false }

    // Tests
    testRuntimeOnly(libs.log4j.core)
    testImplementation(libs.parallelgzip)
}

tasks.test {
    maxHeapSize = "1G"
}

tasks.compileJava {
    dependsOn(":worldedit-libs:build")
    options.compilerArgs.add("-Aarg.name.key.prefix=")
}

tasks.generateGrammarSource {
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
        expand(
                "version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}"
        )
    }
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        artifactId = the<BasePluginExtension>().archivesName.get()
        from(components["java"])
    }
}
