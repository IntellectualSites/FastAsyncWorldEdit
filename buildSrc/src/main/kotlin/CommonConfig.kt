import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the

fun Project.applyCommonConfiguration() {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        maven {
            name = "IntellectualSites"
            url = uri("https://mvn.intellectualsites.com/content/groups/public/")
            content {
                includeGroup("com.plotsquared")
                includeGroup("com.intellectualsites.paster")
                includeGroup("com.github.intellectualsites.plotsquared")
            }
        }
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
            content {
                includeGroupByRegex("org.enginehub.*")
                includeGroupByRegex("com.sk89q.*")
            }
        }
        maven {
            name = "OSS Sonatype Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            name = "OSS Sonatype Releases"
            url = uri("https://oss.sonatype.org/content/repositories/releases/")
        }
        maven {
            name = "Athion"
            url = uri("https://ci.athion.net/plugin/repository/tools/")
            content {
                includeGroup("com.massivecraft")
                includeGroup("com.thevoxelbox.voxelsniper")
                includeGroup("com.palmergames.bukkit")
                includeGroup("net.fabiozumbi12")
                includeGroupByRegex("com.destroystokyo.*")
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(5, "MINUTES")
        }
    }

    plugins.withId("java") {
        the<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
        constraints {
            for (conf in configurations.names) {
                add(conf, "com.google.guava:guava") {
                    version { strictly(Versions.GUAVA) }
                    because("Mojang provides Guava")
                }
                add(conf, "com.google.code.gson:gson") {
                    version { strictly(Versions.GSON) }
                    because("Mojang provides Gson")
                }
                add(conf, "it.unimi.dsi:fastutil") {
                    version { strictly(Versions.FAST_UTIL) }
                    because("Mojang provides FastUtil")
                }
            }
        }
    }
}
