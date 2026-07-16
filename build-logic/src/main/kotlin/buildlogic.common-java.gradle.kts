import buildlogic.stringyLibs
import buildlogic.getLibrary

plugins {
    id("eclipse")
    id("idea")
    id("buildlogic.common")
}

tasks
    .withType<JavaCompile>()
    .matching { it.name == "compileJava" || it.name == "compileTestJava" }
    .configureEach {
        // TODO: re-enable this-escape when ANTLR suppresses it properly
        val disabledLint = listOf(
            "processing", "path", "fallthrough", "serial", "overloads", "this-escape",
        )
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.release.set(25)
        options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
        options.isDeprecation = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("--add-modules=jdk.incubator.vector")
    }

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}

dependencies {
    "compileOnly"(stringyLibs.getLibrary("jsr305"))
    "testImplementation"(platform(stringyLibs.getLibrary("junit-bom")))
    "testImplementation"(stringyLibs.getLibrary("junit-jupiter-api"))
    "testImplementation"(stringyLibs.getLibrary("junit-jupiter-params"))
    "testImplementation"(stringyLibs.getLibrary("jqwik"))
    "testImplementation"(platform(stringyLibs.getLibrary("mockito-bom")))
    "testImplementation"(stringyLibs.getLibrary("mockito-core"))
    "testImplementation"(stringyLibs.getLibrary("mockito-junit-jupiter"))
    "testRuntimeOnly"(stringyLibs.getLibrary("junit-jupiter-engine"))
    "testRuntimeOnly"(stringyLibs.getLibrary("junit-platform-launcher"))
}

// Java 8 turns on doclint which we fail
tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        addStringOption("-add-modules", "jdk.incubator.vector")
        addBooleanOption("Xdoclint:-missing", true)
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
        links(
                "https://jd.advntr.dev/api/latest/",
                "https://logging.apache.org/log4j/2.x/javadoc/log4j-api/",
                "https://www.antlr.org/api/Java/",
                "https://jd.papermc.io/paper/1.21.10/",
                "https://intellectualsites.github.io/fastasyncworldedit-javadocs/worldedit-core/"
        )
        docTitle = "${rootProject.name}-${project.description}" +  " " + "${rootProject.version}"
    }
}

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}

// Generating javadoc for every module is a large, pure overhead on a regular
// `build`/`assemble` (doclint is already off, so it validates almost nothing).
// Only run it when the javadoc jar is actually needed - i.e. when publishing, signing,
// or when the javadoc/javadocJar task is requested explicitly. Published artifacts
// still contain full javadoc; only local build speed changes. `enabled` is set to
// a plain boolean (no captured script reference) so it stays configuration-cache safe.
val javadocRequested = gradle.startParameter.taskNames.any {
    it.contains("publish", ignoreCase = true) || it.contains("sign", ignoreCase = true) || it.endsWith("javadoc") || it.endsWith("javadocJar")
}
tasks.withType<Javadoc>().configureEach {
    enabled = javadocRequested
}
tasks.matching { it.name == "javadocJar" }.configureEach {
    enabled = javadocRequested
}
