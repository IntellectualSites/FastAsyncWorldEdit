import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType

fun Project.applyCommonJavaConfiguration(sourcesJar: Boolean, banSlf4j: Boolean = true) {
    applyCommonConfiguration()
    apply(plugin = "eclipse")
    apply(plugin = "idea")

    tasks
            .withType<JavaCompile>()
            .matching { it.name == "compileJava" || it.name == "compileTestJava" }
            .configureEach {
                val disabledLint = listOf(
                        "processing", "path", "fallthrough", "serial", "overloads", "this-escape",
                )
                options.release.set(17)
                options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
                options.isDeprecation = true
                options.encoding = "UTF-8"
                options.compilerArgs.add("-parameters")
            }

    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.10.0")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.10.0")
        "testImplementation"("org.mockito:mockito-core:5.4.0")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.4.0")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    }

    // Java 8 turns on doclint which we fail
    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            tags(
                    "apiNote:a:API Note:",
                    "implSpec:a:Implementation Requirements:",
                    "implNote:a:Implementation Note:"
            )
            options.encoding = "UTF-8"
            links(
                    "https://jd.advntr.dev/api/latest/",
                    "https://logging.apache.org/log4j/2.x/javadoc/log4j-api/",
                    "https://www.antlr.org/api/Java/",
                    "https://jd.papermc.io/paper/1.20.6/",
                    "https://intellectualsites.github.io/fastasyncworldedit-javadocs/worldedit-core/"
            )
            docTitle = "${rootProject.name}-${project.description}" +  " " + "${rootProject.version}"
        }
    }

    configure<JavaPluginExtension> {
        disableAutoTargetJvm()
        withJavadocJar()
        if (sourcesJar) {
            withSourcesJar()
        }
    }

    if (banSlf4j) {
        configurations["compileClasspath"].apply {
            resolutionStrategy.componentSelection {
                withModule("org.slf4j:slf4j-api") {
                    reject("No SLF4J allowed on compile classpath")
                }
            }
        }
    }
}
