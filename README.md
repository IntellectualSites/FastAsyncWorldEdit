# FastAsyncWorldEdit
[![Join us on Discord](https://img.shields.io/discord/268444645527126017.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/intellectualsites)
[![bStats Servers](https://img.shields.io/bstats/servers/1403)](https://bstats.org/plugin/bukkit/FastAsyncWorldEdit/1403)
[![Crowdin](https://badges.crowdin.net/e/4a5819fae3fd88234a8ea13bfbb072bb/localized.svg)](https://intellectualsites.crowdin.com/fastasyncworldedit)

## What is FAWE and why should I use it?

FAWE is designed for efficient world editing.
* Simple to set up and use
* Extremely configurable
* Uses minimal CPU/Memory
* Safe for many players to use
* Insanely fast, when using the slowest mode

FastAsyncWorldEdit is a fork of WorldEdit that has huge speed and memory improvements and considerably more features.  
If you use other plugins which depend on WorldEdit, simply having FAWE installed will boost their performance.

## Downloads

Downloads are available either on SpigotMC, Modrinth or on CurseForge.
- [Jenkins](https://ci.athion.net/job/FastAsyncWorldEdit/)
- [Modrinth](https://modrinth.com/plugin/fastasyncworldedit/)
- [CurseForge](https://dev.bukkit.org/projects/fawe)

## Features

* Over 200 Commands
* Style and translate messages and commands
* (No setup required) Clipboard web integration (Clipboard)
* Unlimited //undo, per world history, instant lookups/rollback and cross server clipboards
* Advanced per player limits (entity, tiles, memory, changes, iterations, regions, inventory)
* Visualization, targeting modes/masks and scroll actions
* Adds lots of powerful new //brushes and //tools.
* Adds a lot more mask functionality. (new mask syntax, patterns, expressions, source masks)
* Adds a lot more pattern functionality. (a lot of new pattern syntax and patterns)
* Adds edit transforms (apply transforms to a source, e.g. on //paste)
* Adds support for new formats (e.g. Structure Blocks)
* Instant copying of arbitrary size with `//lazycopy`
* Auto repair partially corrupt schematic files
* Biome mixing, in-game world painting, dynamic view distance, vanilla cui, off axis rotation, image importing, cave generation,
  multi-clipboards, interactive messages, schematic visualization, lag prevention, persistent brushes + A LOT MORE

### Performance

There are several placement modes, each supporting higher throughput than the previous. All editing is processed
asynchronously, with
certain tasks being broken up on the main thread. The default mode is chunk placement.
* Blocks (Bukkit-API) - Only used if chunk placement isn't supported. Still faster than any other plugin on spigot.
* Chunks (NMS) - Places entire chunk sections
* World (CFI) - Used to generate new worlds / regions

### Protection Plugins

The following plugins are supported with Bukkit:
* [WorldGuard](https://dev.bukkit.org/projects/worldguard)
* [PlotSquared](https://www.spigotmc.org/resources/77506/)

### Logging and Rollback

By default you can use `//inspect` and `//history rollback` to search and restore changes. To reduce disk usage, increase the
compression level and buffer size. To bypass logging use `//fast`.

### Developer API

FAWE maintains API compatibility with WorldEdit, so you can use the normal WorldEdit API asynchronously.
FAWE also has some asynchronously wrappers for the Bukkit API.
The wiki has examples for various things like reading NBT, modifying world files, pasting schematics, splitting up tasks, lighting etc.
If you need help with anything, hop on discord (link on the left bar).

## Documentation

* [Wiki](https://intellectualsites.github.io/fastasyncworldedit-documentation/)
* [Javadocs](https://intellectualsites.github.io/fastasyncworldedit-javadocs/)

## Contributing

Want to add new features to FastAsyncWorldEdit or fix bugs yourself? You can get the game running, with FastAsyncWorldEdit, from the code here:

For additional information about compiling FastAsyncWorldEdit, read the [compiling documentation](https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/main/COMPILING.adoc).

## Special thanks


[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSource)
<br>
The creators of IntelliJ IDEA, supports us with their Open Source Licenses.

<a href="https://yourkit.com/"><img src="https://www.yourkit.com/images/yklogo.png" width="200">
</a>

Thank you to YourKit for supporting our product by providing us with their innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).