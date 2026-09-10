tasks.register("build") {
    // Some worldedit-libs subprojects may be auto-discovered by Gradle without a build script
    // (e.g. stale output directories for platforms not included in this branch's settings).
    // Skip subprojects that do not define a "build" task instead of failing configuration.
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })
}
