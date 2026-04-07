import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    id("buildlogic.adapter")
}
//
//java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

dependencies {
    // https://artifactory.papermc.io/ui/native/universe/io/papermc/paper/dev-bundle/
    the<PaperweightUserDependenciesExtension>().paperDevBundle("26.1.1.build.+")
    compileOnly(libs.paperLib)
}
