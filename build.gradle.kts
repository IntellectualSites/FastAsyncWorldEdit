import org.ajoberstar.grgit.Grgit
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import java.net.URI

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("xyz.jpenilla.run-paper") version "1.0.6"
}

logger.lifecycle("""
*******************************************
 You are building FastAsyncWorldEdit!

 If you encounter trouble:
 1) Read COMPILING.adoc if you haven't yet
 2) Try running 'build' in a separate Gradle run
 3) Use gradlew and not gradle
 4) If you still need help, ask on Discord! https://discord.gg/intellectualsites

 Output files will be in [subproject]/build/libs
*******************************************
""")

var rootVersion by extra("2.4.9")
var snapshot by extra("SNAPSHOT")
var revision: String by extra("")
var buildNumber by extra("")
var date: String by extra("")
ext {
    val git: Grgit = Grgit.open {
        dir = File("$rootDir/.git")
    }
    date = git.head().dateTime.format(DateTimeFormatter.ofPattern("yy.MM.dd"))
    revision = "-${git.head().abbreviatedId}"
    buildNumber = if (project.hasProperty("buildnumber")) {
        snapshot + "-" + project.properties["buildnumber"] as String
    } else {
        project.properties["snapshot"] as String
    }
}

version = String.format("%s-%s", rootVersion, buildNumber)

if (!project.hasProperty("gitCommitHash")) {
    apply(plugin = "org.ajoberstar.grgit")
    ext["gitCommitHash"] = try {
        extensions.getByName<Grgit>("grgit").head()?.abbreviatedId
    } catch (e: Exception) {
        logger.warn("Error getting commit hash", e)

        "no.git.id"
    }
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType(JavaCompile::class) {
            options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
        }
        tasks.withType(Test::class) {
            testLogging {
                events(FAILED)
                exceptionFormat = FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }
        }
    }
}

applyCommonConfiguration()

tasks {
    runServer {
        minecraftVersion("1.19")
        pluginJars(project(":worldedit-bukkit").file("build/libs/FastAsyncWorldEdit-Bukkit-$version.jar"))

    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(URI.create("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
