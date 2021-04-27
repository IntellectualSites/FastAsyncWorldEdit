Compiling
=========

You can compile FastAsyncWorldEdit as long as you have some version of Java greater than or equal to 8 installed. Gradle will download JDK 8 specifically if needed,
but it needs some version of Java to bootstrap from.

Note that if you have JRE 8 installed, Gradle will currently attempt to use that to compile, which will not work. It is easiest to uninstall JRE 8 and replace it with JDK 8.

You can get the JDK 8 [here](https://adoptopenjdk.net/?variant=openjdk8&jvmVariant=hotspot).

The build process uses Gradle, which you do *not* need to download. FastAsyncWorldEdit is a multi-module project with three active modules:

* `worldedit-core` contains the FastAsyncWorldEdit API
* `worldedit-bukkit` is the Bukkit plugin
* `worldedit-cli` is the command line interface

## To compile...

### NMS
FastAsyncWorldEdit uses NMS (net.minecraft.server) code in a variety of spots. NMS is not distributed via maven and therefore FastAsyncWorldEdit may not build without errors if you didn't install it into your local repository beforehand.
You can do that by either running Spigot's [BuildTools](https://www.spigotmc.org/wiki/buildtools/) targetting the versions needed or using Paper's paperclip with `java -Dpaperclip.install=true -jar paperclip.jar`.

### On Windows

1. Shift + right click the folder with FastAsyncWorldEdit's files and click "Open command prompt".
2. `gradlew build -x test`

### On Linux, BSD, or Mac OS X

1. In your terminal, navigate to the folder with FastAsyncWorldEdit's files (`cd /folder/of/fawe/files`)
2. `./gradlew build -x test`

## Then you will find...

You will find:

* The core FastAsyncWorldEdit API in **worldedit-core/build/libs**
* FastAsyncWorldEdit for Bukkit in **worldedit-bukkit/build/libs**
* the CLI version in **worldedit-cli/build/libs**

If you want to use FastAsyncWorldEdit, use the `FastAsyncWorldEdit-1.16-<commitHash>` version obtained in **worldedit-bukkit/build/libs**.

(The `-#` version includes FastAsyncWorldEdit + necessary libraries.)

## Other commands

* `gradlew idea` will generate an [IntelliJ IDEA](http://www.jetbrains.com/idea/) module for each folder.

* `gradlew build -x test` skips JUnit tests for compiling.

_Possibly broken_:
* `gradlew eclipse` will generate an [Eclipse](https://www.eclipse.org/downloads/) project for each folder.

