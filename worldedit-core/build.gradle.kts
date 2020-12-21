import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.mendhak.gradlecrowdin.DownloadTranslationsTask
import com.mendhak.gradlecrowdin.UploadSourceFileTask
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("java-library")
    id("net.ltgt.apt-eclipse")
    id("net.ltgt.apt-idea")
    id("antlr")
    id("com.mendhak.gradlecrowdin")
}

repositories {
    maven { url = uri("https://plotsquared.com/mvn") }
    maven { url = uri("https://mvn.intellectualsites.com/content/groups/public/") }
    mavenCentral()

}

applyPlatformAndCoreConfiguration()

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:30.1-jre")
    }
}

dependencies {
    "api"(project(":worldedit-libs:core"))
    "implementation"("de.schlichtherle:truezip:6.8.3")
    "implementation"("net.java.truevfs:truevfs-profile-default_2.13:0.12.1")
    "implementation"("org.mozilla:rhino-runtime:1.7.12")
    "implementation"("org.yaml:snakeyaml:1.23")
    "implementation"("com.google.guava:guava:${Versions.GUAVA}")
    "implementation"("com.google.code.findbugs:jsr305:3.0.2")
    "implementation"("com.google.code.gson:gson:${Versions.GSON}")
    "implementation"("org.slf4j:slf4j-api:1.7.27")
    "implementation"("it.unimi.dsi:fastutil:${Versions.FAST_UTIL}")

    val antlrVersion = "4.7.2"
    "antlr"("org.antlr:antlr4:$antlrVersion")
    "implementation"("org.antlr:antlr4-runtime:$antlrVersion")

    "implementation"("com.googlecode.json-simple:json-simple:1.1.1") { isTransitive = false }
    "compileOnly"(project(":worldedit-libs:core:ap"))
    "annotationProcessor"(project(":worldedit-libs:core:ap"))
    // ensure this is on the classpath for the AP
    "annotationProcessor"("com.google.guava:guava:21.0")
    "compileOnly"("com.google.auto.value:auto-value-annotations:${Versions.AUTO_VALUE}")
    "annotationProcessor"("com.google.auto.value:auto-value:${Versions.AUTO_VALUE}")
    "testImplementation"("ch.qos.logback:logback-core:${Versions.LOGBACK}")
    "testImplementation"("ch.qos.logback:logback-classic:${Versions.LOGBACK}")
    "compile"("com.github.luben:zstd-jni:1.4.3-1")
    "compileOnly"("net.fabiozumbi12:redprotect:1.9.6")
    "compile"("com.github.intellectualsites.plotsquared:PlotSquared-API:latest") {
        isTransitive = false
    }
    "compile"("com.plotsquared:PlotSquared-Core:5.13.3") {
        isTransitive = false
    }
    "api"("com.intellectualsites.paster:Paster:1.0-SNAPSHOT")
}

tasks.named<Test>("test") {
    maxHeapSize = "1G"
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

sourceSets.named("main") {
    java {
        srcDir("src/main/java")
        srcDir("src/legacy/java")
    }
    resources {
        srcDir("src/main/resources")
    }
}

tasks.named<Copy>("processResources") {
    filesMatching("fawe.properties") {
        expand("version" to "$version",
                "commit" to "${rootProject.ext["revision"]}",
                "date" to "${rootProject.ext["date"]}")
    }
}
tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("com.github.luben:zstd-jni:1.4.3-1"))

    }
}

val crowdinApiKey = "crowdin_apikey"

if (project.hasProperty(crowdinApiKey) && !gradle.startParameter.isOffline) {
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

    val dlTranslationsTask = tasks.named<DownloadTranslationsTask>("crowdinDownload") {
        apiKey = "${project.property(crowdinApiKey)}"
        destination = "${buildDir.resolve("crowdin-i18n")}"
        projectId = "worldedit-core"
    }

    tasks.named<Copy>("processResources") {
        dependsOn(dlTranslationsTask)
        from(dlTranslationsTask.get().destination) {
            into("lang")
        }
    }

    tasks.named("classes") {
        dependsOn("crowdinDownload")
    }
}
