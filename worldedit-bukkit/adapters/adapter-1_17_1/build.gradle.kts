import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

applyPaperweightAdapterConfiguration()

plugins {
    java
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

configurations.all {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
}


dependencies {
    // url=https://repo.papermc.io/service/rest/repository/browse/maven-public/io/papermc/paper/dev-bundle/1.17.1-R0.1-SNAPSHOT
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.17.1-R0.1-20220414.034903-210")
    compileOnly(libs.paperlib)
}
