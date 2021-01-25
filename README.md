<p align="center">
  <img src="fawe-logo.png" width="300">
</p>

---

FAWE is a fork of WorldEdit that has huge speed and memory improvements and considerably more features

**A Minecraft Map Editor... that runs in-game!**

* With selections, schematics, copy and paste, brushes, and scripting!
* Use it in creative, survival in single player or on your server.
* Use it on your Minecraft server to fix grieving and mistakes.

Java Edition required. WorldEdit is compatible with Forge, Fabric, Bukkit, Spigot, Paper, and Sponge.

## Download FastAsyncWorldEdit
### 1.15+
* [Download](https://www.spigotmc.org/resources/fast-async-worldedit.13932/)
* [Jenkins](https://ci.athion.net/job/FastAsyncWorldEdit-1.16/)

Looking builds for older versions? Download them [here](https://intellectualsites.github.io/download/).

## Links

* [Spigot Page](https://www.spigotmc.org/threads/fast-async-worldedit.100104/)
* [Discord](https://discord.gg/KxkjDVg)
* [Wiki](https://wiki.intellectualsites.com/FastAsyncWorldEdit/index)
* [Report Issue](https://github.com/IntellectualSites/FastAsyncWorldEdit/issues)
* [Crowdin](https://intellectualsites.crowdin.com/fastasyncworldedit)
* [JavaDocs](https://ci.athion.net/job/FastAsyncWorldEdit-1.16/javadoc/)

Edit the Code
---------

Want to add new features to WorldEdit or fix bugs yourself? You can get the game running, with WorldEdit, from the code here, without any additional outside steps, by doing the following *four* things:

1. Download WorldEdit's source code and put it somewhere. We recommend you use something called Git if you already know how to use it, but [you can also just download a .zip file](https://github.com/EngineHub/WorldEdit/archive/master.zip). (If you plan on contributing the changes, you will need to figure out Git.)
2. Install any version of Java greater than or equal to 8.
   * Note that if you do _not_ install JDK 8 exactly, Gradle will download it for you on first run. However, it is still required to have some form of Java installed for Gradle to start at all.
3. Open terminal / command prompt / bash and navigate to the directory where you put the source code.
4. Run **one** of these following commands:
   * Mac OS X / Linux: `./gradlew :worldedit-fabric:runClient`
   * Windows - Command Prompt: `gradlew :worldedit-fabric:runClient`
   * Windows - PowerShell: `.\gradlew :worldedit-fabric:runClient`

🎉 That's it. 🎉 It takes a long time to actually transform WorldEdit into a mod. If it succeeds, **the Minecraft game will open and you can create a single player world with WorldEdit**.

---

For additional information about compiling WorldEdit, see [COMPILING.md](COMPILING.md).
FAWE is a fork of WorldEdit that has huge speed and memory improvements and considerably more features

## Building
FAWE uses gradle to build  
You can safely ignore `gradlew setupDecompWorkspace` if you are not planning to work on the forge side of FAWE.

```
$ gradlew setupDecompWorkspace
$ gradlew build
```

The jar is located in `worldedit-bukkit/build/libs/FastAsyncWorldEdit-1.16-###.jar`

## Contributing
Have an idea for an optimization, or a cool feature?
 - We will accept most PR's
 - Let us know what you've tested / what may need further testing
 - If you need any help, create a ticket or discuss on [Discord](https://discord.gg/KxkjDVg)

## YourKit
<a href="https://www.yourkit.com">
  <img src="https://www.yourkit.com/images/yklogo.png">
</a>
</br>
Thank you to YourKit for supporting our product by providing us with their innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>, <a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>, and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>
