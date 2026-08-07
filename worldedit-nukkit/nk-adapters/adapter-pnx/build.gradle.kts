plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "PowerNukkitX Releases"
        url = uri("https://repo.powernukkitx.org/releases/")
    }
}

dependencies {
    compileOnly(project(":worldedit-nukkit"))
    compileOnly("org.powernukkitx:server:2.0.0-SNAPSHOT")
    compileOnly("org.cloudburstmc:nbt:3.0.3.Final")
}
