import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

fun Project.applyPlatformAndCoreConfiguration() {
    applyCommonConfiguration()
    apply(plugin = "java")
    apply(plugin = "eclipse")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "signing")

    applyCommonJavaConfiguration(
            sourcesJar = name in setOf("worldedit-core", "worldedit-bukkit"),
    )

    if (project.hasProperty("buildnumber")) {
        ext["internalVersion"] = "$version;${rootProject.ext["gitCommitHash"]}"
    } else {
        ext["internalVersion"] = "$version"
    }

    configure<JavaPluginExtension> {
        disableAutoTargetJvm()
        withJavadocJar()
    }

    if (name in setOf("worldedit-core", "worldedit-bukkit", "worldedit-cli")) {
        the<JavaPluginExtension>().withSourcesJar()
    }

    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }

    val publishingExtension = the<PublishingExtension>()

    configure<SigningExtension> {
        if (!version.toString().endsWith("-SNAPSHOT")) {
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
            isRequired
            sign(publishingExtension.publications)
        }
    }

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>("maven") {
                from(javaComponent)

                group = "com.fastasyncworldedit"
                artifactId = "${rootProject.name}-${project.description}"
                version = version

                pom {
                    name.set("${rootProject.name}-${project.description}" + " " + project.version)
                    description.set("Blazingly fast Minecraft world manipulation for artists, builders and everyone else.")
                    url.set("https://github.com/IntellectualSites/FastAsyncWorldEdit")

                    licenses {
                        license {
                            name.set("GNU General Public License, Version 3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("NotMyFault")
                            name.set("Alexander Brandes")
                            email.set("contact(at)notmyfault.dev")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                        developer {
                            id.set("SirYwell")
                            name.set("Hannes Greule")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                        developer {
                            id.set("dordsor21")
                            name.set("dordsor21")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                    }

                    scm {
                        url.set("https://github.com/IntellectualSites/FastAsyncWorldEdit")
                        connection.set("scm:git:https://github.com/IntellectualSites/FastAsyncWorldEdit.git")
                        developerConnection.set("scm:git:git@github.com:IntellectualSites/FastAsyncWorldEdit.git")
                        tag.set("${project.version}")
                    }

                    issueManagement{
                        system.set("GitHub")
                        url.set("https://github.com/IntellectualSites/FastAsyncWorldEdit/issues")
                    }
                }
            }
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
    tasks.withType<ShadowJar>().configureEach {
        relocate("com.sk89q.jchronic", "com.sk89q.worldedit.jchronic")
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
    .flatMap { listOf(it, "FastAsyncWorldEdit/$it", "../$it", "../FastAsyncWorldEdit/$it") }
    .joinToString(separator = " ")

sealed class WorldEditKind(
    val name: String,
    val mainClass: String = "com.sk89q.worldedit.internal.util.InfoEntryPoint"
) {
    class Standalone(mainClass: String) : WorldEditKind("STANDALONE", mainClass)
    object Mod : WorldEditKind("MOD")
    object Plugin : WorldEditKind("PLUGIN")
}

fun Project.addJarManifest(kind: WorldEditKind, includeClasspath: Boolean = false, extraAttributes: Map<String, String> = mapOf()) {
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
        attributes.putAll(extraAttributes)
        manifest.attributes(attributes)
    }
}
