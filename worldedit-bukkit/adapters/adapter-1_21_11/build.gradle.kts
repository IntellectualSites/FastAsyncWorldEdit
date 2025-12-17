import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("buildlogic.adapter")
}

configurations {
    all {
        resolutionStrategy {
            capabilitiesResolution {
                withCapability("org.lz4:lz4-java") {
                    selectHighestVersion()
                }
            }
        }
    }
}

dependencies {
    // https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.21.11-R0.1-20251209.225848-3")
    compileOnly(libs.paperLib)
}
