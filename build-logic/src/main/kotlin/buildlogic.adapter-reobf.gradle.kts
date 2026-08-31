import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("buildlogic.adapter")
}

paperweight {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named("assemble") {
    dependsOn("reobfJar")
}

tasks
    .withType<JavaCompile>()
    .matching { it.name == "compileJava" || it.name == "compileTestJava" }
    .configureEach {
        // We use Java 21 for most of the pre-existing adapters.
        options.release.set(21)
    }
