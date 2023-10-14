import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import javax.inject.Inject

fun Project.applyLibrariesConfiguration() {
    applyCommonConfiguration()
    apply(plugin = "java-base")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "signing")

    configurations {
        create("shade")
    }

    group = "${rootProject.group}.worldedit-libs"

    val relocations = mapOf(
        "net.kyori.text" to "com.sk89q.worldedit.util.formatting.text",
        "net.kyori.minecraft" to "com.sk89q.worldedit.util.kyori",
        "net.kyori.adventure.nbt" to "com.sk89q.worldedit.util.nbt"

    )

    tasks.register<ShadowJar>("jar") {
        configurations = listOf(project.configurations["shade"])
        archiveClassifier.set("")

        dependencies {
            exclude(dependency("com.google.guava:guava"))
            exclude(dependency("com.google.code.gson:gson"))
            exclude(dependency("com.google.errorprone:error_prone_annotations"))
            exclude(dependency("org.checkerframework:checker-qual"))
            exclude(dependency("org.apache.logging.log4j:log4j-api"))
            exclude(dependency("com.google.code.findbugs:jsr305"))
        }

        relocations.forEach { (from, to) ->
            relocate(from, to)
        }
    }
    val altConfigFiles = { artifactType: String ->
        val deps = configurations["shade"].incoming.dependencies
            .filterIsInstance<ModuleDependency>()
            .map { it.copy() }
            .map { dependency ->
                dependency.artifact {
                    name = dependency.name
                    type = artifactType
                    extension = "jar"
                    classifier = artifactType
                }
                dependency
            }

        files(configurations.detachedConfiguration(*deps.toTypedArray())
            .resolvedConfiguration.lenientConfiguration.artifacts
            .filter { it.classifier == artifactType }
            .map { zipTree(it.file) })
    }
    tasks.register<Jar>("sourcesJar") {
        from({
            altConfigFiles("sources")
        })
        relocations.forEach { (from, to) ->
            val filePattern = Regex("(.*)${from.replace('.', '/')}((?:/|$).*)")
            val textPattern = Regex.fromLiteral(from)
            eachFile {
                filter {
                    it.replaceFirst(textPattern, to)
                }
                path = path.replaceFirst(filePattern, "$1${to.replace('.', '/')}$2")
            }
        }
        archiveClassifier.set("sources")
    }

    // This a dummy jar to comply with the requirements of the OSSRH,
    // libs are not API and therefore no "proper" javadoc jar is necessary
    tasks.register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }

    tasks.named("assemble").configure {
        dependsOn("jar", "sourcesJar", "javadocJar")
    }

    project.apply<LibsConfigPluginHack>()

    val libsComponent = project.components["libs"] as AdhocComponentWithVariants

    val apiElements = project.configurations.register("apiElements") {
        isVisible = false
        description = "API elements for libs"
        isCanBeResolved = false
        isCanBeConsumed = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
        outgoing.artifact(tasks.named("jar"))
    }

    val runtimeElements = project.configurations.register("runtimeElements") {
        isVisible = false
        description = "Runtime elements for libs"
        isCanBeResolved = false
        isCanBeConsumed = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
        outgoing.artifact(tasks.named("jar"))
    }

    val sourcesElements = project.configurations.register("sourcesElements") {
        isVisible = false
        description = "Source elements for libs"
        isCanBeResolved = false
        isCanBeConsumed = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.SOURCES))
        }
        outgoing.artifact(tasks.named("sourcesJar"))
    }

    val javadocElements = project.configurations.register("javadocElements") {
        isVisible = false
        description = "Javadoc elements for libs"
        isCanBeResolved = false
        isCanBeConsumed = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
            attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.SHADOWED))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.JAVADOC))
        }
        outgoing.artifact(tasks.named("javadocJar"))
    }

    libsComponent.addVariantsFromConfiguration(apiElements.get()) {
        mapToMavenScope("compile")
    }

    libsComponent.addVariantsFromConfiguration(runtimeElements.get()) {
        mapToMavenScope("runtime")
    }

    libsComponent.addVariantsFromConfiguration(sourcesElements.get()) {
        mapToMavenScope("runtime")
    }

    libsComponent.addVariantsFromConfiguration(javadocElements.get()) {
        mapToMavenScope("runtime")
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
                from(libsComponent)

                group = "com.fastasyncworldedit"
                artifactId = "FastAsyncWorldEdit-Libs-${project.name.replaceFirstChar(Char::titlecase)}"
                version = version

                pom {
                    name.set("${rootProject.name}-Libs" + " " + project.version)
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
                        }
                        developer {
                            id.set("SirYwell")
                            name.set("Hannes Greule")
                            organization.set("IntellectualSites")
                        }
                        developer {
                            id.set("dordsor21")
                            name.set("dordsor21")
                            organization.set("IntellectualSites")
                        }
                    }

                    scm {
                        url.set("https://github.com/IntellectualSites/FastAsyncWorldEdit")
                        connection.set("scm:git:https://github.com/IntellectualSites/FastAsyncWorldEdit.git")
                        developerConnection.set("scm:git:git@github.com:IntellectualSites/FastAsyncWorldEdit.git")
                        tag.set("${project.version}")
                    }

                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/IntellectualSites/FastAsyncWorldEdit/issues")
                    }
                }
            }
        }
    }

}

// A horrible hack because `softwareComponentFactory` has to be gotten via plugin
// gradle why
internal open class LibsConfigPluginHack @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        val libsComponents = softwareComponentFactory.adhoc("libs")
        project.components.add(libsComponents)
    }
}

fun Project.constrainDependenciesToLibsCore() {
    evaluationDependsOn(":worldedit-libs:core")
    val coreDeps = project(":worldedit-libs:core").configurations["shade"].dependencies
        .filterIsInstance<ExternalModuleDependency>()
    dependencies.constraints {
        for (coreDep in coreDeps) {
            add("shade", "${coreDep.group}:${coreDep.name}:${coreDep.version}") {
                because("libs should align with libs:core")
            }
        }
    }
}
