import org.ajoberstar.grgit.Grgit
import java.time.format.DateTimeFormatter

plugins {
    id("com.gradle.build-scan") version "2.4.1"
}

logger.lifecycle("""
*******************************************
 You are building FastAsyncWorldEdit!

 If you encounter trouble:
 1) Read COMPILING.md if you haven't yet
 2) Try running 'build' in a separate Gradle run
 3) Use gradlew and not gradle
 4) If you still need help, ask on Discord! https://discord.gg/enginehub

 Output files will be in [subproject]/build/libs
*******************************************
""")
//TODO FIX THIS WHEN I FEEL LIKE IT
var rootVersion = "1.14"
var revision: String = ""
var buildNumber = ""
var date: String = ""
ext {
    val git: Grgit = Grgit.open {
        dir = File(rootDir.toString() + "/.git");
    }
    ext["date"] = git.head().dateTime.format(DateTimeFormatter.ofPattern("yy.MM.dd"));
    ext["revision"] = "-${git.head().abbreviatedId}";
    var parents: MutableList<String>? = git.head().parentIds;
    if (project.hasProperty("buildnumber")) {
        buildNumber = project.properties["buildnumber"] as String;
    } else {
        var index = -2109;  // Offset to match CI
        while (parents != null && parents.isNotEmpty()) {
            parents = git.getResolve().toCommit(parents.get(0)).getParentIds()
            index++;
        }
        buildNumber = index.toString();
    }
}
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

version = String.format("%s-%s", rootVersion, buildNumber)

if (!project.hasProperty("gitCommitHash")) {
    apply(plugin = "org.ajoberstar.grgit")
    ext["gitCommitHash"] = try {
        (ext["grgit"] as Grgit?)?.head()?.abbreviatedId
    } catch (e: Exception) {
        logger.warn("Error getting commit hash", e)

        "no_git_id"
    }
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")

    publishAlways()
}
