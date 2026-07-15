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

var rootVersion by extra("2.15.4")
var snapshot by extra("SNAPSHOT")
var revision: String by extra("")
var buildNumber by extra("")
var date: String by extra("")
ext {
    // In a `git worktree` checkout (used e.g. by parallel agent sandboxes), `.git` is a pointer
    // file rather than the git directory itself, and JGit (used by Grgit) does not resolve the
    // worktree's `commondir` indirection the way native git does, so Grgit.open() throws even
    // though `git` commands work fine in the same directory. Fall back to placeholder date/
    // revision values in that case (these are informational only, used in fawe.properties/
    // version string) - but only in the detected-worktree case, so a genuinely broken .git in a
    // normal checkout still fails the build loudly instead of silently shipping "no.git.id".
    val isWorktreeCheckout = File("$rootDir/.git").isFile
    try {
        val git: Grgit = Grgit.open {
            dir = File("$rootDir/.git")
        }
        date = git.head().dateTime.format(DateTimeFormatter.ofPattern("yy.MM.dd"))
        revision = "-${git.head().abbreviatedId}"
    } catch (e: Exception) {
        if (!isWorktreeCheckout) {
            throw e
        }
        logger.warn("Error opening git repository for date/revision (worktree checkout); using placeholders", e)
        date = DateTimeFormatter.ofPattern("yy.MM.dd").format(java.time.LocalDate.now())
        revision = "-no.git.id"
    }
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

val totalReport = tasks.register<JacocoReport>("jacocoTotalReport") {
    for (proj in subprojects) {
        proj.apply(plugin = "jacoco")
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
            minecraftVersion(it)
            pluginJars(*project(":worldedit-bukkit").getTasksByName("shadowJar", false).map { (it as Jar).archiveFile }
                    .toTypedArray())
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true", "--add-modules=jdk.incubator.vector")
            group = "run paper"
            runDirectory.set(file("run-$it"))
        }
    }
    runServer<RunServer> {
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
