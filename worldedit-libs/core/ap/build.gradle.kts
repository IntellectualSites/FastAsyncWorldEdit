applyLibrariesConfiguration()

dependencies {
    "shade"("FAWE-Piston:core/build/libs/core-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/annotations/build/libs/annotations-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/processor/build/libs/processor-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("org.enginehub.piston.core-ap:annotations:${Versions.PISTON}")
    "shade"("org.enginehub.piston.core-ap:processor:${Versions.PISTON}")
}
