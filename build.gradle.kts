import org.ajoberstar.grgit.Grgit

logger.lifecycle("""
*******************************************
 You are building WorldEdit!

 If you encounter trouble:
 1) Read COMPILING.md if you haven't yet
 2) Try running 'build' in a separate Gradle run
 3) Use gradlew and not gradle
 4) If you still need help, ask on Discord! https://discord.gg/enginehub

 Output files will be in [subproject]/build/libs
*******************************************
""")

//def rootVersion = "1.13"
//def revision = ""
//def buildNumber = ""
//def date = ""
//ext {
//    git = Grgit.open(dir: new File(rootDir.toString()+"/.git"))
//    date = git.head().getDate().format("yy.MM.dd")
//    revision = "-${git.head().abbreviatedId}"
//    parents = git.head().parentIds;
//    if (project.hasProperty("buildnumber")) {
//        buildNumber = "$buildnumber"
//    } else {
//        index = -2109;  // Offset to match CI
//        for (; parents != null && !parents.isEmpty(); index++) {
//            parents = git.getResolve().toCommit(parents.get(0)).getParentIds()
//        }
//        buildNumber = "${index}"
//    }
//}
//
//version = String.format("%s.%s", rootVersion, buildNumber)

if (!project.hasProperty("gitCommitHash")) {
    apply(plugin = "org.ajoberstar.grgit")
    ext["gitCommitHash"] = try {
        (ext["grgit"] as Grgit?)?.head()?.abbreviatedId
    } catch (e: Exception) {
        logger.warn("Error getting commit hash", e)

        "no_git_id"
    }
}
