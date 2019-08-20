applyLibrariesConfiguration()

dependencies {
    "shade"("FAWE-Piston:core/build/libs/core-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/runtime/build/libs/runtime-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:default-impl/build/libs/default-impl-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("org.enginehub.piston.core-ap:annotations:${Versions.PISTON}")
    "shade"("org.enginehub.piston.core-ap:processor:${Versions.PISTON}")
}
