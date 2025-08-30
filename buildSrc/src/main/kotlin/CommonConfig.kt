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
        mavenCentral()
        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
            content {
                excludeModule("io.papermc.paper", "dev-bundle")
            }
        }
        maven {
            name = "OSS Sonatype Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            name = "Athion"
            url = uri("https://ci.athion.net/plugin/repository/tools/")
        }
        maven {
            name = "IntellectualSites"
            url = uri("https://repo.intellectualsites.dev/repository/paper-dev-bundles/")
            content {
                includeModule("io.papermc.paper", "dev-bundle")
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
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        constraints {
            for (conf in configurations) {
                if (conf.isCanBeConsumed || conf.isCanBeResolved) {
                    // dependencies don't get declared in these
                    continue
                }
                add(conf.name, "com.google.guava:guava") {
                    version { require("33.3.1-jre") }
                    because("Mojang provides Guava")
                }
                add(conf.name, "com.google.code.gson:gson") {
                    version { require("2.11.0") }
                    because("Mojang provides Gson")
                }
                add(conf.name, "it.unimi.dsi:fastutil") {
                    version { require("8.5.15") }
                    because("Mojang provides FastUtil")
                }
            }
        }
    }
}
