Compiling
=========

You can compile FastAsyncWorldEdit as long as you have the [Java Development Kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) for Java 8.
Newer versions of the JDK may not compile. 
You only need one version of the JDK installed.

The build process uses Gradle, which you do *not* need to download. FastAsyncWorldEdit is a multi-module project with two active modules:

* `worldedit-core` contains the FastAsyncWorldEdit API
* `worldedit-bukkit` is the Bukkit plugin

## To compile...

### On Windows

1. Shift + right click the folder with FastAsyncWorldEdit's files and click "Open command prompt".
2. `gradlew build`

### On Linux, BSD, or Mac OS X

1. In your terminal, navigate to the folder with FastAsyncWorldEdit's files (`cd /folder/of/fawe/files`)
2. `./gradlew build`

## Then you will find...

You will find:

* The core FastAsyncWorldEdit API in **worldedit-core/build/libs**
* FastAsyncWorldEdit for Bukkit in **worldedit-bukkit/build/libs***

If you want to use FastAsyncWorldEdit, use the `FastAsyncWorldEdit-1.15-#` version.

## Other commands

* `gradlew idea` will generate an [IntelliJ IDEA](http://www.jetbrains.com/idea/) module for each folder.

_Possibly broken_:
* `gradlew eclipse` will generate an [Eclipse](https://www.eclipse.org/downloads/) project for each folder.

