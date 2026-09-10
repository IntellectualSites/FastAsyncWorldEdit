plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "OpenCollab Releases"
        url = uri("https://repo.opencollab.dev/maven-releases/")
    }
    maven {
        name = "OpenCollab Snapshots"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
}

dependencies {
    compileOnly(project(":worldedit-nukkit"))
    compileOnly("cn.nukkit:nukkit:1.0-SNAPSHOT")
}
