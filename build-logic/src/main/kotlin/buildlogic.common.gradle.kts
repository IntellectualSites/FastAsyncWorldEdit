import buildlogic.getLibrary
import buildlogic.stringyLibs
import org.gradle.plugins.ide.idea.model.IdeaModel

group = rootProject.group
version = rootProject.version

configurations.configureEach {
    resolutionStrategy {
        cacheChangingModulesFor(1, TimeUnit.DAYS)
    }
}

// Forge 1.7.10 port: Minecraft 1.7.10 ships log4j-api 2.0-beta9, which lacks the fixed-arity Logger overloads
// (e.g. info(String, Object, Object)) that javac picks against newer APIs. Building with
// -Pfawe.log4jApiVersion=2.0-beta9 compiles against that API so calls bind to the varargs overloads instead.
val forcedLog4jApiVersion = providers.gradleProperty("fawe.log4jApiVersion")
if (forcedLog4jApiVersion.isPresent) {
    val forced = "org.apache.logging.log4j:log4j-api:${forcedLog4jApiVersion.get()}"
    configurations.configureEach {
        resolutionStrategy.force(forced)
    }
}

plugins.withId("java") {
    the<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    for (conf in listOf("implementation", "api")) {
        if (!configurations.names.contains(conf)) {
            continue
        }
        add(conf, platform(stringyLibs.getLibrary("log4j-bom")).map {
            val dep = create(it)
            dep.because("Mojang provides Log4j")
            dep
        })
        constraints {
            add(conf, stringyLibs.getLibrary("guava")) {
                version { require("33.3.1-jre") }
                because("Mojang provides Guava")
            }
            add(conf, stringyLibs.getLibrary("gson")) {
                version { require("2.11.0") }
                because("Mojang provides Gson")
            }
            add(conf, stringyLibs.getLibrary("fastutil")) {
                version { require("8.5.15") }
                because("Mojang provides FastUtil")
            }
        }
    }
}

plugins.withId("idea") {
    configure<IdeaModel> {
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }
}
