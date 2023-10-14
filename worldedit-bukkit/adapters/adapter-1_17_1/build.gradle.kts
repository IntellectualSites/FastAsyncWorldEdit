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
    the<PaperweightUserDependenciesExtension>().paperDevBundle("1.17.1-R0.1-20220414.034903-210")
    compileOnly(libs.paperlib)
}
