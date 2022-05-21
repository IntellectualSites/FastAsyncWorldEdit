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
                        "processing", "path", "fallthrough", "serial"
                )
                options.release.set(17)
                options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
                options.isDeprecation = true
                options.encoding = "UTF-8"
                options.compilerArgs.add("-parameters")
            }

    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.8.1")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.8.1")
        "testImplementation"("org.mockito:mockito-core:3.12.4")
        "testImplementation"("org.mockito:mockito-junit-jupiter:3.12.4")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.8.1")
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
                    "https://javadoc.io/doc/com.google.code.findbugs/jsr305/latest/index.html",
                    "https://jd.adventure.kyori.net/api/latest/",
                    "https://javadoc.io/doc/org.apache.logging.log4j/log4j-api/latest/index.html",
                    "https://www.antlr.org/api/Java/",
                    "https://docs.enginehub.org/javadoc/org.enginehub.piston/core/0.5.7/",
                    "https://docs.enginehub.org/javadoc/org.enginehub.piston/default-impl/0.5.7/",
                    "https://jd.papermc.io/paper/1.18/",
                    "https://javadoc.io/doc/com.fastasyncworldedit/FastAsyncWorldEdit-Core" // needed for other module linking
            )
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
