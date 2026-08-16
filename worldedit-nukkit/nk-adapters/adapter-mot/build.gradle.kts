plugins {
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "repo-lanink-cn"
        url = uri("https://repo.lanink.cn/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":worldedit-nukkit"))
    compileOnly("cn.nukkit:Nukkit:MOT-SNAPSHOT")
}
