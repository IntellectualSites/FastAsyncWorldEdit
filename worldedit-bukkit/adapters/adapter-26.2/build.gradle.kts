import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("buildlogic.adapter")
}

// workaround LeafPile providing wrong snakeyaml version
configurations.named("testRuntimeClasspath") {
    exclude(group = "org.yaml", module = "snakeyaml")
}

dependencies {
    // https://artifactory.papermc.io/ui/native/universe/io/papermc/paper/dev-bundle/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("26.2.build.+")
    compileOnly(libs.paperLib)
    testRuntimeOnly(libs.snakeyaml) {
        because("Keep adapter tests on Bukkit's SnakeYaml while excluding LeafPile's conflicting transitive version")
    }
}
