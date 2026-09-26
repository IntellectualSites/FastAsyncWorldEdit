import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    `java-library`
    antlr
    id("buildlogic.core-and-platform")
}

project.description = "Core"

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

// Resolve these to plain Strings up front. Referencing `version` or `rootProject`
// inside the filesMatching action would capture a Project instance in the task,
// which cannot be serialized by the configuration cache.
val faweVersion = "$version"
val faweCommit = "${rootProject.ext["revision"]}"
val faweDate = "${rootProject.ext["date"]}"

tasks.named<Copy>("processResources") {
    // Collect into a local of THIS block. A top-level `val` in a .gradle.kts is a
    // property of the script object, so referencing one from the filesMatching
    // closure would capture the script itself - which the configuration cache
    // cannot serialize. A local is captured by value instead.
    val expansions = mapOf(
            "version" to faweVersion,
            "commit" to faweCommit,
            "date" to faweDate
    )
    inputs.property("faweVersion", faweVersion)
    inputs.property("faweCommit", faweCommit)
    inputs.property("faweDate", faweDate)
    filesMatching("fawe.properties") {
        expand(expansions)
    }
}

configure<PublishingExtension> {
    publications.named<MavenPublication>("maven") {
        artifactId = the<BasePluginExtension>().archivesName.get()
        from(components["java"])
    }
}
