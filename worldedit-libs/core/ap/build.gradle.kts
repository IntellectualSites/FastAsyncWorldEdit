applyLibrariesConfiguration()

dependencies {
    "shade"("FAWE-Piston:core/build/libs/core-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/annotations/build/libs/annotations-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("FAWE-Piston:core-ap/processor/build/libs/processor-${Versions.PISTON}:lastSuccessfulBuild@jar")
    "shade"("org.enginehub.piston.core-ap:annotations:0.4.3")
    "shade"("org.enginehub.piston.core-ap:processor:0.4.3")
}
