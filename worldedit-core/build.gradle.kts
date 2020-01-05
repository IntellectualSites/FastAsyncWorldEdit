import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaModel
import com.mendhak.gradlecrowdin.DownloadTranslationsTask
import com.mendhak.gradlecrowdin.UploadSourceFileTask

plugins {
    id("java-library")
     id("java")
    id("maven")
    id("maven-publish")
    id("net.ltgt.apt-eclipse")
    id("net.ltgt.apt-idea")
    id("antlr")
    id("com.mendhak.gradlecrowdin")
}

repositories {
    maven { url = uri("http://ci.athion.net/job/PlotSquared-breaking/ws/mvn/") }

}

applyPlatformAndCoreConfiguration()

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:21.0")
    }
}

dependencies {
    "compile"(project(":worldedit-libs:core"))
    "compile"("de.schlichtherle:truezip:6.8.3")
    "compile"("org.mozilla:rhino:1.7.11")
    "compile"("org.yaml:snakeyaml:1.23")
    "compile"("com.google.guava:guava:21.0")
    "compile"("com.google.code.findbugs:jsr305:3.0.2")
    "compile"("com.google.code.gson:gson:2.8.0")
    "compile"("org.slf4j:slf4j-api:1.7.26")
    "compile"("it.unimi.dsi:fastutil:8.2.1")

    val antlrVersion = "4.7.2"
    "antlr"("org.antlr:antlr4:$antlrVersion")
    "implementation"("org.antlr:antlr4-runtime:$antlrVersion")

    "compile"("com.googlecode.json-simple:json-simple:1.1.1") { isTransitive = false }
    "compileOnly"(project(":worldedit-libs:core:ap"))
    "annotationProcessor"(project(":worldedit-libs:core:ap"))
    // ensure this is on the classpath for the AP
    "annotationProcessor"("com.google.guava:guava:21.0")
    "compileOnly"("com.google.auto.value:auto-value-annotations:${Versions.AUTO_VALUE}")
    "annotationProcessor"("com.google.auto.value:auto-value:${Versions.AUTO_VALUE}")
    "compile"("co.aikar:fastutil-lite:1.0")
    "compile"("com.github.luben:zstd-jni:1.4.3-1")
    "compileOnly"("net.fabiozumbi12:redprotect:1.9.6")
    "compile"("com.github.intellectualsites.plotsquared:PlotSquared-API:latest") {
        isTransitive = false
    }
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

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir("src/legacy/java")
        }
        resources {
            srcDir("src/main/resources")
        }
    }
}

tasks.named<Copy>("processResources") {
    filesMatching("fawe.properties") {
        expand("version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}")
    }
}

val jar: Jar by tasks
jar.archiveName = "FAWE-API-${project.version}.jar"
jar.destinationDir = file("../mvn/com/boydti/FAWE-API/" + project.version)

task("writeNewPom") {
    doLast {
        maven.pom {
            withGroovyBuilder {
                "project" {
                    groupId = "com.boydti"
                    artifactId = "FAWE-API"
                    version = "project.version"
                }
            }
        }.writeTo("../mvn/com/boydti/FAWE-API/${project.version}/FAWE-API-${project.version}.pom")
        maven.pom {
            withGroovyBuilder {
                "project" {
                    groupId = "com.boydti"
                    artifactId = "FAWE-API"
                    version = "latest"
                }
            }
        }.writeTo("../mvn/com/boydti/FAWE-API/latest/FAWE-API-latest.pom")
    }
}

task("dataContent") {
    doLast {
        copySpec {
            from("../mvn/com/boydti/FAWE-API/${project.version}/")
            into("../mvn/com/boydti/FAWE-API/latest/")
            include("*.jar")
            rename("FAWE-API-${project.version}.jar", "FAWE-API-latest.jar")
        }
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("com.github.luben:zstd-jni:1.4.3-1"))

    }
}

val crowdinApiKey = "crowdin_apikey"

if (project.hasProperty(crowdinApiKey)) {
    tasks.named<UploadSourceFileTask>("crowdinUpload") {
        apiKey = "${project.property(crowdinApiKey)}"
        projectId = "worldedit-core"
        files = arrayOf(
            object {
                var name = "strings.json"
                var source = "${file("src/main/resources/lang/strings.json")}"
            }
        )
    }

    tasks.named<DownloadTranslationsTask>("crowdinDownload") {
        apiKey = "${project.property(crowdinApiKey)}"
        destination = "${file("build/resources/main/lang")}"
        projectId = "worldedit-core"
    }

    tasks.named("classes").configure {
        dependsOn("crowdinDownload")
    }
}

tasks.named("dataContent").configure {
    dependsOn("writeNewPom")
}
