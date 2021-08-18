import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

fun Project.applyPlatformAndCoreConfiguration() {
    applyCommonConfiguration()
    apply(plugin = "java")
    apply(plugin = "eclipse")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    if (project.hasProperty("buildnumber")) {
        ext["internalVersion"] = "$version;${rootProject.ext["gitCommitHash"]}"
    } else {
        ext["internalVersion"] = "$version"
    }

    tasks
        .withType<JavaCompile>()
        .matching { it.name == "compileJava" || it.name == "compileTestJava" }
        .configureEach {
            val disabledLint = listOf(
                "processing", "path", "fallthrough", "serial"
            )
            options.release.set(11)
            options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
            options.isDeprecation = false
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }

    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.7.2")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.7.2")
        "testImplementation"("org.mockito:mockito-core:3.11.2")
        "testImplementation"("org.mockito:mockito-junit-jupiter:3.11.2")
        "testImplementation"("net.bytebuddy:byte-buddy:1.11.9")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:3.11.2")
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
                    "https://javadoc.io/doc/com.google.code.findbugs/jsr305/3.0.2/",
                    "https://jd.adventure.kyori.net/api/4.8.1/",
                    "https://javadoc.io/doc/org.apache.logging.log4j/log4j-api/2.14.1/",
                    "https://javadoc.io/doc/com.google.guava/guava/21.0/",
                    "https://www.antlr.org/api/Java/",
                    "https://docs.enginehub.org/javadoc/org.enginehub.piston/core/0.5.7/",
                    "https://docs.enginehub.org/javadoc/org.enginehub.piston/default-impl/0.5.7/"
            )
        }
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn("javadoc")
        archiveClassifier.set(null as String?)
        archiveFileName.set("${rootProject.name}-${project.description}-${project.version}-javadoc.${archiveExtension.getOrElse("jar")}")
        from(tasks.getByName<Javadoc>("javadoc").destinationDir)
    }

    tasks.named("assemble").configure {
        dependsOn("javadocJar")
    }

    artifacts {
        add("archives", tasks.named("jar"))
        add("archives", tasks.named("javadocJar"))
    }

    if (name == "worldedit-core" || name == "worldedit-bukkit") {
        tasks.register<Jar>("sourcesJar") {
            dependsOn("classes")
            archiveClassifier.set(null as String?)
            archiveFileName.set("${rootProject.name}-${project.description}-${project.version}-sources.${archiveExtension.getOrElse("jar")}")
            from(sourceSets["main"].allSource)
        }

        artifacts {
            add("archives", tasks.named("sourcesJar"))
        }
        tasks.named("assemble").configure {
            dependsOn("sourcesJar")
        }
    }

    if (name != "worldedit-fabric") {
        configurations["compileClasspath"].apply {
            resolutionStrategy.componentSelection {
                withModule("org.slf4j:slf4j-api") {
                    reject("No SLF4J allowed on compile classpath")
                }
            }
        }
    }

}

fun Project.applyShadowConfiguration() {
    tasks.named<ShadowJar>("shadowJar") {
        dependencies {
            include(project(":worldedit-libs:core"))
            include(project(":worldedit-libs:${project.name.replace("worldedit-", "")}"))
            include(project(":worldedit-core"))
            exclude("com.google.code.findbugs:jsr305")
        }
        exclude("GradleStart**")
        exclude(".cache")
        exclude("LICENSE*")
        exclude("META-INF/maven/**")
        minimize()
    }
}

val CLASSPATH = listOf("truezip", "truevfs", "js")
    .map { "$it.jar" }
    .flatMap { listOf(it, "FastAsyncWorldEdit/$it") }
    .joinToString(separator = " ")

sealed class WorldEditKind(
    val name: String,
    val mainClass: String = "com.sk89q.worldedit.internal.util.InfoEntryPoint"
) {
    class Standalone(mainClass: String) : WorldEditKind("STANDALONE", mainClass)
    object Mod : WorldEditKind("MOD")
    object Plugin : WorldEditKind("PLUGIN")
}

fun Project.addJarManifest(kind: WorldEditKind, includeClasspath: Boolean = false) {
    tasks.named<Jar>("jar") {
        val version = project(":worldedit-core").version
        inputs.property("version", version)
        val attributes = mutableMapOf(
            "Implementation-Version" to version,
            "WorldEdit-Version" to version,
            "WorldEdit-Kind" to kind.name,
            "Main-Class" to kind.mainClass
        )
        if (includeClasspath) {
            attributes["Class-Path"] = CLASSPATH
        }
        manifest.attributes(attributes)
    }
}
