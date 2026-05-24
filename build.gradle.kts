import org.ajoberstar.grgit.Grgit
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import java.time.format.DateTimeFormatter
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    alias(libs.plugins.codecov)
    jacoco
    id("buildlogic.common")
    id("com.gradleup.nmcp.aggregation") version "1.6.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val rootVersion: String = (extra.properties["rootVersion"] as? String) ?: "2.15.4"
val snapshot: String = (extra.properties["snapshot"] as? String) ?: "SNAPSHOT"
var revision: String = (extra.properties["revision"] as? String) ?: ""
var buildNumber: String = (extra.properties["buildNumber"] as? String) ?: ""
var date: String = (extra.properties["date"] as? String) ?: ""

val git: Grgit = Grgit.open {
    dir = File("$rootDir/.git")
}
date = git.head().dateTime.format(DateTimeFormatter.ofPattern("yy.MM.dd"))
revision = "-${git.head().abbreviatedId}"
    buildNumber = if (project.hasProperty("buildnumber")) {
        snapshot + "-" + (project.findProperty("buildnumber") as? String ?: "")
    } else {
        (project.findProperty("snapshot") as? String) ?: snapshot
    }

extra.set("rootVersion", rootVersion)
extra.set("snapshot", snapshot)
extra.set("revision", revision)
extra.set("buildNumber", buildNumber)
extra.set("date", date)

version = String.format("%s-%s", rootVersion, buildNumber)

if (!project.hasProperty("gitCommitHash")) {
    pluginManager.apply("org.ajoberstar.grgit")
    ext["gitCommitHash"] = try {
        extensions.getByName<Grgit>("grgit").head()?.abbreviatedId
    } catch (e: Exception) {
        logger.warn("Error getting commit hash", e)

        "no.git.id"
    }
}

val totalReport = tasks.register<JacocoReport>("jacocoTotalReport") {
    description = "Generates a combined JaCoCo coverage report for all subprojects."
    for (proj in subprojects) {
        proj.pluginManager.apply("jacoco")
        proj.plugins.withId("java") {
            executionData(
                    fileTree(proj.layout.buildDirectory).include("**/jacoco/*.exec")
            )
            sourceSets(proj.the<JavaPluginExtension>().sourceSets["main"])
            reports {
                xml.required.set(true)
                xml.outputLocation.set(rootProject.layout.buildDirectory.file("reports/jacoco/report.xml"))
                html.required.set(true)
            }
            dependsOn(proj.tasks.named("test"))
        }
    }
}
afterEvaluate {
    totalReport.configure {
        classDirectories.setFrom(classDirectories.files.map {
            fileTree(it).apply {
                exclude("**/*AutoValue_*")
                exclude("**/*Registration.*")
            }
        })
    }
}

codecov {
    reportTask.set(totalReport)
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
        }
        tasks.withType<Test>().configureEach {
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
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

val supportedVersions: List<String> = listOf("1.21", "1.21.1", "1.21.4", "1.21.5",
        "1.21.8", "1.21.10", "1.21.11", "26.1.2", "26.2")

tasks {
    supportedVersions.forEach {
        register<RunServer>("runServer-$it") {
            description = "Run a Paper server version $it."
            minecraftVersion(it)
            pluginJars(*project(":worldedit-bukkit").getTasksByName("shadowJar", false).map { (it as Jar).archiveFile }
                    .toTypedArray())
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true", "--add-modules=jdk.incubator.vector")
            group = "run paper"
            runDirectory.set(file("run-$it"))
        }
    }
    runServer<RunServer> {
        description = "Run a Paper server for the latest supported Minecraft version (${supportedVersions.last()})."
        minecraftVersion(supportedVersions.last())
        pluginJars(*project(":worldedit-bukkit").getTasksByName("shadowJar", false).map { (it as Jar).archiveFile }
                .toTypedArray())
        jvmArgs("-Dcom.mojang.eula.agree=true")

    }
}

nmcpAggregation {
    centralPortal {
        publishingType = "AUTOMATIC"
        username = providers.gradleProperty("mavenCentralUsername")
        password = providers.gradleProperty("mavenCentralPassword")
    }

    publishAllProjectsProbablyBreakingProjectIsolation()
}
