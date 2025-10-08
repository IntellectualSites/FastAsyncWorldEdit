# Running Development Server

This document explains how to run a development Minecraft server with your plugin for testing.

## Gradle (Recommended)

The Gradle setup uses the `run-paper` plugin and **does not require a full build**:

```bash
# Run server with default version (1.21.8)
./gradlew runServer

# Run server with specific version
./gradlew runServer-1.21.4
./gradlew runServer-1.20.4
```

**Key advantage:** The `runServer` task only depends on `shadowJar` from the `worldedit-bukkit` module, not the full reactor build. This means:
- Faster iteration during development
- Only compiles changed modules
- Automatically copies the plugin JAR to the server

### Supported Versions
- 1.20.4, 1.20.5, 1.20.6
- 1.21, 1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.9, 1.21.10

## Maven

Maven has been configured with the equivalent `run-paper` plugin. The Maven POMs have been updated to include necessary code generation steps (ANTLR parsers, Piston command annotations).

```bash
# Build the plugin and run server (downloads Paper automatically)
mvn clean package -pl worldedit-bukkit -am -DskipTests -Prun-server
```

**Note:** The first run may take longer as it downloads the Paper server JAR (~100MB).

### Why the difference?

- **Gradle:** Task-based system allows declaring dependencies on specific tasks (like `shadowJar`) without triggering parent tasks. The run-paper plugin handles everything automatically.
- **Maven:** Lifecycle-based system requires explicit phase bindings. The current implementation downloads Paper and copies plugins automatically, but still requires a build step first.

## Configuration Details

Both setups:
- Use Paper server version 1.21.10 by default
- Accept EULA automatically via `-Dcom.mojang.eula.agree=true`
- Enable Java preview features
- Add incubator vector module for performance
- Store server files in `run/` directory (or `run-<version>/` for Gradle version-specific tasks)

## Tips for Development

### Gradle Workflow
```bash
# Edit code
# Run server (automatically compiles and packages)
./gradlew runServer
```

### Maven Workflow
```bash
# Edit code
# Build and run server (downloads Paper on first run)
mvn clean package -pl worldedit-bukkit -am -DskipTests -Prun-server
```

### Quick Rebuild (Maven)
If you want to rebuild only the changed module:
```bash
# For worldedit-bukkit changes
mvn package -pl worldedit-bukkit -am -DskipTests

# For worldedit-core changes (bukkit depends on it)
mvn package -pl worldedit-bukkit -am -DskipTests
```

The `-am` (also-make) flag ensures dependencies are built too.
The `-pl` (projects list) flag specifies which module to build.

## Server Directory Structure

```
run/                          # Server directory
├── plugins/
│   └── FastAsyncWorldEdit-Paper-*.jar
├── server.jar               # Downloaded Paper server
├── eula.txt                # Auto-accepted
└── world/                   # World data
```
