tasks.register("build") {
    subprojects.forEach { subproject ->
        dependsOn(subproject.tasks.matching { it.name == "build" })
    }
}
