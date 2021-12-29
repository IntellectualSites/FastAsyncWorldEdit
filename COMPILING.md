Compiling
=========

You can compile FastAsyncWorldEdit as long as you have some version of Java greater than or equal to 17 installed. Gradle will download JDK 17 specifically if needed,
but it needs some version of Java to bootstrap from.

Note that if you have JRE 8 installed, Gradle will currently attempt to use that to compile, which will not work. It is easiest to uninstall JRE 8 and replace it with JDK 16.

You can get the JDK 17 [here](https://adoptium.net/).

The build process uses Gradle, which you do *not* need to download. FastAsyncWorldEdit is a multi-module project with three active modules:

* `worldedit-core` contains the FastAsyncWorldEdit API
* `worldedit-bukkit` is the Bukkit plugin
* `worldedit-cli` is the command line interface

## To compile...

### On Windows

1. Shift + right-click the folder with FastAsyncWorldEdit's files and click "Open command prompt".
2. `gradlew clean build`

### On Linux, BSD, or Mac OS X

1. In your terminal, navigate to the folder with FastAsyncWorldEdit's files (`cd /folder/of/fawe/files`)
2. `./gradlew clean build`

## Then you will find...

You will find:

* The core FastAsyncWorldEdit API in **worldedit-core/build/libs**
* FastAsyncWorldEdit for Bukkit in **worldedit-bukkit/build/libs**
* the CLI version in **worldedit-cli/build/libs**

If you want to use FastAsyncWorldEdit, use the `FastAsyncWorldEdit-<identifier>` version obtained in **worldedit-bukkit/build/libs**.

(The `-#` version includes FastAsyncWorldEdit + necessary libraries.)

## Other commands

* `gradlew idea` will generate an [IntelliJ IDEA](https://www.jetbrains.com/idea/) module for each folder.

_Possibly broken_:
* `gradlew eclipse` will generate an [Eclipse](https://www.eclipse.org/downloads/) project for each folder.

