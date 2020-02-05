import org.gradle.api.Project
import org.gradle.kotlin.dsl.repositories

fun Project.applyCommonConfiguration() {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("http://ci.athion.net/job/PlotSquared-breaking/ws/mvn/") }
        maven { url = uri("https://maven.sk89q.com/repo/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("http://empcraft.com/maven2") }
        maven { url = uri("https://repo.destroystokyo.com/repository/maven-public") }
        ivy { url = uri("https://ci.athion.net/job")
            patternLayout {
                artifact("/[organisation]/[revision]/artifact/[module].[ext]")
            }}
    }
    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(5, "minutes")
        }
    }
}
