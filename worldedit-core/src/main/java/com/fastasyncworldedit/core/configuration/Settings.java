package com.fastasyncworldedit.core.configuration;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The settings of FAWE's {@code config.yml}.
 * <p>
 * Settings are immutable. Loading or changing them creates a new instance, which takes effect once passed to
 * {@link #set(Settings)}, so always read through {@link #settings()} rather than keeping hold of an instance or one of
 * its sections.
 * <p>
 * Each record is a section of the file and each component a setting, stored under the component's name in kebab case
 * and in declaration order. A new setting only needs a component with a {@link Comment} and a {@link Default}.
 */
public record Settings(
        @Comment({
                "Set true to enable WorldEdit restrictions per region (e.g. PlotSquared or WorldGuard).",
                "To be allowed to WorldEdit in a region, users need the appropriate",
                "fawe.<plugin>  permission. See the Permissions page for supported region plugins."
        })
        @Default("true")
        boolean regionRestrictions,
        @Comment({
                "FAWE will cancel non admin edits when memory consumption exceeds this %",
                " - Bypass with `/wea` or `//fast` or `fawe.bypass`",
                " - Disable with 100 or -1."
        })
        @Default("95")
        int maxMemoryPercent,
        @Comment({
                "When percent memory usage reaches this threshold some aspects of editing will be slowed down:",
                " - FAWE-Asynchronous chunk loading when writing changes (see queue.async-chunk-load-write)"
        })
        @Default("80")
        int slowerMemoryPercent,
        EnabledComponents enabledComponents,
        Clipboard clipboard,
        Lighting lighting,
        TickLimiter tickLimiter,
        Web web,
        Extent extent,
        Experimental experimental,
        Queue queue,
        History history,
        Paths paths,
        RegionRestrictionsOptions regionRestrictionsOptions,
        General general,
        @Default(Settings.DEFAULT_LIMIT)
        Map<String, Limits> limits
) {

    /**
     * The name of the limit group that applies to actors without a permission for any other group.
     */
    public static final String DEFAULT_LIMIT = "default";

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    // Written to the file for people reading it, never read back
    private static final List<String> INFORMATION = List.of("issues", "wiki");

    // Not volatile: settings are immutable, so a thread that has not yet seen a reload reads the previous ones, whole
    private static Settings instance = defaults();

    public Settings {
        Map<String, Limits> groups = new LinkedHashMap<>(limits);
        if (!groups.containsKey(DEFAULT_LIMIT)) {
            groups.put(DEFAULT_LIMIT, defaults().defaultLimits());
        }
        limits = Collections.unmodifiableMap(groups);
    }

    /**
     * {@return the settings in use}
     *
     * @since 2.0.0
     */
    public static Settings settings() {
        return instance;
    }

    /**
     * Puts settings into use.
     *
     * @apiNote This is an internal method, use with care. It is not thread-safe and does not check the settings are valid.
     * @since 3.0.0
     */
    @ApiStatus.Internal
    public static void set(Settings settings) {
        instance = Objects.requireNonNull(settings);
    }

    /**
     * Changes one of the settings in use, until the configuration is next loaded. The file is not changed.
     *
     * @param option a reference to the accessor of the setting, e.g. {@code Settings.Queue::parallelThreads}
     * @param value  the new value
     * @throws IllegalArgumentException if the setting is not given as a reference to its accessor
     * @since 3.0.0
     */
    public static <S extends Record> void override(Option.Bool<S> option, boolean value) {
        overrideOption(option, value);
    }

    /**
     * @see #override(Option.Bool, boolean)
     * @since 3.0.0
     */
    public static <S extends Record> void override(Option.Int<S> option, int value) {
        overrideOption(option, value);
    }

    /**
     * @see #override(Option.Bool, boolean)
     * @since 3.0.0
     */
    public static <S extends Record> void override(Option.Text<S> option, String value) {
        overrideOption(option, value);
    }

    /**
     * @see #override(Option.Bool, boolean)
     * @since 3.0.0
     */
    public static <S extends Record> void override(Option.List<S> option, List<String> value) {
        overrideOption(option, value);
    }

    /**
     * @see #override(Option.Bool, boolean)
     * @since 3.0.0
     */
    public static <S extends Record, E extends Enum<E>> void override(Option.Choice<S, E> option, E value) {
        overrideOption(option, value);
    }

    private static synchronized void overrideOption(Option option, Object value) {
        instance = instance.with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * {@return the settings of a new installation}
     * <p>
     * These are the {@link Default} of each setting, apart from the few below that depend on where FAWE runs.
     *
     * @since 3.0.0
     */
    public static Settings defaults() {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        return ConfigMapper.defaults(Settings.class, Map.of(
                "queue.parallel-threads", processors,
                "queue.target-size", 8 * processors,
                "queue.thread-target-size-percent", Queue.threadTargetSizePercent(processors),
                // Every limit group starts from the limits of WorldEdit's own configuration, which is loaded first
                "limits.max-radius", LocalConfiguration.MAX_RADIUS,
                "limits.max-super-pickaxe-size", LocalConfiguration.MAX_SUPER_RADIUS,
                "limits.max-brush-radius", LocalConfiguration.MAX_BRUSH_RADIUS,
                "limits.max-butcher-radius", LocalConfiguration.MAX_BUTCHER_RADIUS
        ));
    }

    /**
     * Reads settings from a file without putting them into use.
     * <p>
     * Settings the file leaves out, or sets to something unusable, take their default. A file that is not valid YAML is
     * copied to a file next to it ending in {@code _broken} and the defaults are returned.
     *
     * @param file the file, which does not need to exist
     * @return the settings
     * @throws IOException if the file exists but cannot be read
     * @since 3.0.0
     */
    public static Settings load(Path file) throws IOException {
        return load(file, defaults());
    }

    /**
     * Reads settings from a file without putting them into use, or gives others if the file has none to read.
     * <p>
     * Settings the file leaves out, or sets to something unusable, take their default. A file that does not exist gives
     * the fallback, and so does a file that is not valid YAML, after it is copied to a file next to it ending in
     * {@code _broken}. Passing the settings in use keeps them when a reload finds the file broken.
     *
     * @param file     the file, which does not need to exist
     * @param fallback the settings to give if the file does not exist or is not valid YAML
     * @return the settings
     * @throws IOException if the file exists but cannot be read
     * @since 3.0.0
     */
    public static Settings load(Path file, Settings fallback) throws IOException {
        Objects.requireNonNull(fallback);
        if (Files.notExists(file)) {
            return fallback;
        }
        Map<String, Object> tree;
        try {
            tree = ConfigYaml.read(file);
        } catch (YAMLException e) {
            LOGGER.error("Could not read {}, it was copied to {} and the settings in use are kept",
                    file, ConfigYaml.backup(file), e);
            return fallback;
        }
        Settings defaults = defaults();
        INFORMATION.forEach(tree::remove);
        migrate(new ConfigTree(tree));
        List<String> problems = new ArrayList<>();
        Settings settings = ConfigMapper.bind(tree, defaults, problems);
        if (!problems.isEmpty()) {
            LOGGER.warn("Problems in {}:\n - {}", file.getFileName(), String.join("\n - ", problems));
        }
        return settings;
    }

    /**
     * Moves settings of older versions to where they are now, for files that have not been saved since.
     */
    private static void migrate(ConfigTree tree) {
        // 2.10.0 split the update notification toggle into three
        Object updateNotification = tree.remove("enabled-components.update-notification");
        if (updateNotification != null) {
            tree.putIfAbsent("enabled-components.snapshot-update-notifications", updateNotification);
            tree.putIfAbsent("enabled-components.release-update-notifications", updateNotification);
            tree.putIfAbsent("enabled-components.notify-update-ingame", updateNotification);
        }
        // 2.10.0 moved the schematic directory limits into the limit groups
        moveIntoLimits(tree, "experimental.per-player-file-size-limit", "schem-file-size-limit");
        moveIntoLimits(tree, "experimental.per-player-file-num-limit", "schem-file-num-limit");
        // 2.11.0 added delete-disk-on-logout, which keeps what delete-on-logout did for existing installations
        Object deleteOnLogout = tree.get("history.delete-on-logout");
        if (deleteOnLogout != null) {
            tree.putIfAbsent("history.delete-disk-on-logout", deleteOnLogout);
        }
        // thread-target-size-percent follows the configured thread count rather than the processor count
        if (tree.get("queue.parallel-threads") instanceof Integer threads && threads > 0) {
            tree.putIfAbsent("queue.thread-target-size-percent", Queue.threadTargetSizePercent(threads));
        }
        // 3.0.0 stopped writing the platform, date, build and commit of the running build, which were never read
        tree.remove("platform");
        tree.remove("date");
        tree.remove("build");
        tree.remove("commit");
    }

    private static void moveIntoLimits(ConfigTree tree, String from, String option) {
        Object value = tree.remove(from);
        if (value == null) {
            return;
        }
        // The default group gets it too when the file has other groups but not that one, as it is otherwise unlimited
        tree.putIfAbsent("limits." + DEFAULT_LIMIT + "." + option, value);
        Map<String, Object> groups = tree.section("limits");
        if (groups == null) {
            // Not a section, which binding reports
            return;
        }
        for (String group : groups.keySet()) {
            tree.putIfAbsent("limits." + group + "." + option, value);
        }
    }

    /**
     * Writes the settings to a file, replacing what is in it.
     *
     * @param file the file
     * @since 3.0.0
     */
    public void save(Path file) throws IOException {
        ConfigYaml.Writer out = new ConfigYaml.Writer();
        String[] values = {
                "https://github.com/IntellectualSites/FastAsyncWorldEdit/issues",
                "https://intellectualsites.github.io/fastasyncworldedit-documentation/"
        };
        out.comments(0, List.of("These first 2 aren't configurable"));
        for (int i = 0; i < INFORMATION.size(); i++) {
            out.key(0, INFORMATION.get(i)).scalar(values[i]);
        }
        ConfigMapper.write(this, true, out);
        ConfigYaml.write(file, out);
    }

    /**
     * Creates a copy of the settings with one of them changed.
     *
     * @param option a reference to the accessor of the setting, e.g. {@code Settings.Queue::parallelThreads}
     * @param value  the new value
     * @return the changed settings, which are not in use until passed to {@link #set(Settings)}
     * @throws IllegalArgumentException if the setting is not given as a reference to its accessor
     * @since 3.0.0
     */
    public <S extends Record> Settings with(Option.Bool<S> option, boolean value) {
        return with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * @see #with(Option.Bool, boolean)
     * @since 3.0.0
     */
    public <S extends Record> Settings with(Option.Int<S> option, int value) {
        return with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * @see #with(Option.Bool, boolean)
     * @since 3.0.0
     */
    public <S extends Record> Settings with(Option.Text<S> option, String value) {
        return with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * @see #with(Option.Bool, boolean)
     * @since 3.0.0
     */
    public <S extends Record> Settings with(Option.List<S> option, List<String> value) {
        return with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * @see #with(Option.Bool, boolean)
     * @since 3.0.0
     */
    public <S extends Record, E extends Enum<E>> Settings with(Option.Choice<S, E> option, E value) {
        return with(ConfigMapper.pathOf(Settings.class, option), value);
    }

    /**
     * Creates a copy of the settings with the one at a path of the file changed, e.g. {@code limits.default.max-radius}.
     * This reaches the settings of limit groups, which {@link Option} cannot name.
     *
     * @throws IllegalArgumentException if there is no such setting or the value does not suit it
     */
    Settings with(String path, Object value) {
        return ConfigMapper.with(this, path, value);
    }

    /**
     * {@return the limit group that applies to actors without a permission for any other group}
     *
     * @since 3.0.0
     */
    public Limits defaultLimits() {
        return limits.get(DEFAULT_LIMIT);
    }

    /**
     * Creates the limit of an actor from the limit groups they have permission for.
     *
     * @param actor the actor
     * @return a new limit
     * @since 3.0.0
     */
    public FaweLimit getLimit(Actor actor) {
        return LimitResolver.resolve(limits, actor);
    }

    @Comment("Enable or disable core components")
    public record EnabledComponents(
            @Default("true")
            boolean commands,
            @Comment({
                    "Show additional information in console. It helps us at IntellectualSites to find out more about an issue.",
                    "Leave it off if you don't need it, it can spam your console."
            })
            @Default("false")
            boolean debug,
            @Comment("Whether or not FAWE should notify you on startup about new available snapshots.")
            @Default("true")
            boolean snapshotUpdateNotifications,
            @Comment("Whether or not FAWE should notify you on startup about new releases.")
            @Default("true")
            boolean releaseUpdateNotifications,
            @Comment("Whether or not FAWE should notify you for updates (snapshot / release) on join (with the required permission)")
            @Default("true")
            boolean notifyUpdateIngame
    ) {

    }

    @Comment("Paths for various directories")
    public record Paths(
            @Comment("Put any minecraft or mod jars for FAWE to be aware of block textures")
            @Default("textures")
            String textures,
            @Default("heightmap")
            String heightmap,
            @Default("history")
            String history,
            @Comment({
                    "Multiple servers can use the same clipboards",
                    " - Use a shared directory or NFS/Samba"
            })
            @Default("clipboard")
            String clipboard,
            @Comment("Each player has their own sub directory for schematics")
            @Default("false")
            boolean perPlayerSchematics
    ) {

    }

    @Comment("Region restriction settings")
    public record RegionRestrictionsOptions(
            @Comment({
                    "What type of users are allowed to WorldEdit in a region",
                    " - MEMBER = Players added to a region",
                    " - OWNER = Players who own the region"
            })
            @Default("MEMBER")
            FaweMaskManager.MaskType mode,
            @Comment({
                    "Allow region blacklists.",
                    " - Currently only implemented for WorldGuard",
                    " - see region-restrictions-options.worldguard-region-blacklist"
            })
            @Default("false")
            boolean allowBlacklists,
            @Comment({
                    "List of plugin mask managers that should be exclusive. Exclusive managers are not",
                    "checked for edit restrictions if another manager already allowed an edit, and further",
                    "managers are not checked if an exclusive manager allows an edit.",
                    " - May be useful to add PlotSquared if using both P2 and WorldGuard on a server",
                    " - Some custom-implementations in other plugins may override this setting"
            })
            @Default("ExamplePlugin")
            List<String> exclusiveManagers,
            @Comment({
                    "If a worldguard-protected world should be considered as a region blacklist.",
                    " - This will create a blacklist of regions where an edit cannot operate.",
                    " - Useful for a \"freebuild\" worlds with few protected areas.",
                    " - May cause performance loss with large numbers of protected areas.",
                    " - Requires region-restrictions-options.allow-blacklists be true.",
                    " - Will still search for current allowed regions to limit the edit to.",
                    " - Any blacklist regions are likely to override any internal allowed regions."
            })
            @Default("false")
            boolean worldguardRegionBlacklist,
            @Comment({
                    "Restrict all edits to within the safe chunk limits of +/- 30 million blocks",
                    " - Edits outside this range may induce crashing",
                    " - Forcefully prevents any edit outside this range"
            })
            @Default("true")
            boolean restrictToSafeRange
    ) {

        public RegionRestrictionsOptions {
            exclusiveManagers = List.copyOf(exclusiveManagers);
        }

    }

    @Comment({
            "The \"default\" limit group affects those without a specific limit permission.",
            "To grant someone different limits, copy the default limits group",
            "and give it a different name (e.g. newbie). Then give the user the limit",
            "permission node with that limit name (e.g. fawe.limit.newbie  )"
    })
    public record Limits(
            @Comment("Max actions that can be run concurrently (i.e. commands)")
            @Default("1")
            int maxActions,
            @Comment("Max number of block changes (e.g. by `//set stone`).")
            @Default("50_000_000")
            long maxChanges,
            @Comment("Max number of blocks checked (e.g. `//count stone` which doesn't change blocks)")
            @Default("50_000_000")
            long maxChecks,
            @Comment("Number of times a change can fail (e.g. if the player can't access that region)")
            @Default("50_000_000")
            int maxFails,
            @Comment("Allowed brush iterations (e.g. `//brush smooth`)")
            @Default("1000")
            int maxIterations,
            @Comment("Max allowed entities (e.g. cows)")
            @Default("1337")
            int maxEntities,
            @Comment("Max allowed radius (e.g. for //sphere)")
            // Depends on where FAWE runs, see Settings#defaults()
            int maxRadius,
            @Comment("Max allowed superpickaxe size")
            // Depends on where FAWE runs, see Settings#defaults()
            int maxSuperPickaxeSize,
            @Comment("Max allowed brush radius")
            // Depends on where FAWE runs, see Settings#defaults()
            int maxBrushRadius,
            @Comment("Max allowed butcher radius")
            // Depends on where FAWE runs, see Settings#defaults()
            int maxButcherRadius,
            @Comment({
                    "BlockStates include Banner, Beacon, BrewingStand, Chest, CommandBlock,",
                    "CreatureSpawner, Dispenser, Dropper, EndGateway, Furnace, Hopper, Jukebox,",
                    "NoteBlock, Sign, Skull, Structure"
            })
            @Default("1337")
            int maxBlockstates,
            @Comment({
                    "Maximum size of the player's history in Megabytes:",
                    " - History on disk or memory will be deleted"
            })
            @Default("-1")
            int maxHistoryMb,
            @Comment({
                    "Sets a maximum limit (in kb) for the size of a player's schematics directory (per-player mode only)",
                    "Set to -1 to disable"
            })
            @Default("-1")
            int schemFileSizeLimit,
            @Comment({
                    "Sets a maximum limit for the amount of schematics in a player's schematics directory (per-player mode only)",
                    "Set to -1 to disable"
            })
            @Default("-1")
            int schemFileNumLimit,
            @Comment("Maximum time in milliseconds //calc can execute")
            @Default("50")
            int maxExpressionMs,
            @Comment({
                    "Cinematic block placement:",
                    " - Adds a delay to block placement (nanoseconds/block)",
                    " - Having an artificial delay will use more CPU/Memory"
            })
            @Default("0")
            int speedReduction,
            @Comment({
                    "Place chunks instead of individual blocks:",
                    " - Disabling this will negatively impact performance",
                    " - Only disable this for compatibility or cinematic placement"
            })
            @Default("true")
            boolean fastPlacement,
            @Comment({
                    "Should WorldEdit use inventory?",
                    "0 = No inventory usage (creative)",
                    "1 = Inventory for removing and placing (freebuild)",
                    "2 = Inventory for placing (survival)"
            })
            @Default("0")
            int inventoryMode,
            @Comment("Should large edits require confirmation (>16384 chunks)")
            @Default("true")
            boolean confirmLarge,
            @Comment({
                    "If undo and redo commands should be restricted to allowed regions",
                    " - Prevents scenarios where players can delete/reset a region, and then continue to undo/redo on it"
            })
            @Default("true")
            boolean restrictHistoryToRegions,
            @Comment("List of nbt tags to strip from blocks, e.g. Items")
            @Default({})
            List<String> stripNbt,
            @Comment({
                    "If the disallowed blocks listed in worldedit-config.yml should be disallowed in all edits,",
                    "not just where blocks patterns are used.",
                    " - Can prevent blocks being pasted from clipboards, etc.",
                    " - If fast-placement is disabled, this may cause edits to be slower."
            })
            @Default("true")
            boolean universalDisallowedBlocks,
            @Comment("If legacy, numerical block IDs should be able to be used (i.e. 12:2).")
            @Default("true")
            boolean allowLegacy,
            @Comment({
                    "If sending creature spawn events should be skipped upon creation.",
                    " - Will not work when setting entities directly to a world, rather than using an EditSession(Builder)",
                    " - Setting fast-placement to false forces writing directly to the world (for example)."
            })
            @Default("false")
            boolean skipCreatureSpawnEvents,
            @Comment({
                    "List of blocks to deny use of. Can be either an entire block type or a block with a specific property value.",
                    "Where block properties are specified, any blockstate with the property will be disallowed (e.g. all directions",
                    "of a waterlogged fence). For blocking/remapping of all occurrences of a property like waterlogged, see",
                    "remap-properties below.",
                    "To generate a blank list, substitute the default content with a set of square brackets [] instead.",
                    "The 'worldedit.anyblock' permission is not considered here.",
                    "Example block property blocking:",
                    " - \"minecraft:conduit[waterlogged=true]\"",
                    " - \"minecraft:piston[extended=false,facing=west]\"",
                    " - \"minecraft:wheat[age=7]\""
            })
            @Default({"minecraft:wheat", "minecraft:fire", "minecraft:redstone_wire"})
            List<String> disallowedBlocks,
            @Comment({
                    "List of block properties that should be remapped if used in an edit. Entries should take the form",
                    "\"property_name[value1_old:value1_new,value2_old:value2_new]\". For example:",
                    " - \"waterlogged[true:false]\"",
                    " - \"age[7:4,6:4,5:4]\"",
                    " - \"extended[true:false]\""
            })
            @Default({})
            List<String> remapProperties
    ) {

        public Limits {
            stripNbt = List.copyOf(stripNbt);
            disallowedBlocks = List.copyOf(disallowedBlocks);
            remapProperties = List.copyOf(remapProperties);
        }

    }

    public record History(
            @Comment({
                    "Should history be saved on disk:",
                    " - Frees up a lot of memory",
                    " - Persists restarts",
                    " - Unlimited undo",
                    " - Does not affect edit performance if `combine-stages`"
            })
            @Default("true")
            boolean useDisk,
            @Comment({
                    "Use a database to store disk storage summaries:",
                    " - Enables inspection and rollback",
                    " - Does not impact performance"
            })
            @Default("true")
            boolean useDatabase,
            @Comment({
                    "Record history with dispatching:",
                    " - Much faster as it avoids duplicate block checks",
                    " - Slightly worse compression since dispatch order is different"
            })
            @Default("true")
            boolean combineStages,
            @Comment({
                    "Do not wait for a chunk's history to save before sending it",
                    " - Undo/redo commands will wait until the history has been written to disk before executing",
                    " - Requires combine-stages = true"
            })
            @Default("true")
            boolean sendBeforeHistory,
            @Comment({
                    "Higher compression reduces the size of history at the expense of CPU",
                    "0 = Uncompressed byte array (fastest)",
                    "1 = 1 pass fast compressor (default)",
                    "2 = 2 x fast",
                    "3 = 3 x fast",
                    "4 = 1 x medium, 1 x fast",
                    "5 = 1 x medium, 2 x fast",
                    "6 = 1 x medium, 3 x fast",
                    "7 = 1 x high, 1 x medium, 1 x fast",
                    "8 = 1 x high, 1 x medium, 2 x fast",
                    "9 = 1 x high, 1 x medium, 3 x fast (best compression)",
                    "NOTE: If using disk, do some compression (3+) as smaller files save faster",
                    " - levels over 6 require ZSTD 1.4.8+ to be installed to the system"
            })
            @Default("3")
            int compressionLevel,
            @Comment({
                    "The buffer size for compression:",
                    " - Larger = better ratio but uses more upfront memory",
                    " - Must be in the range [64, 33554432]"
            })
            @Default("531441")
            int bufferSize,
            @Comment("Delete history on disk after a number of days")
            @Default("7")
            int deleteAfterDays,
            @Comment("Delete history in memory on logout (does not effect disk)")
            @Default("true")
            boolean deleteOnLogout,
            @Comment("Delete history on disk on logout")
            @Default("false")
            boolean deleteDiskOnLogout,
            @Comment({
                    "If history should be enabled by default for plugins using WorldEdit:",
                    " - It is faster to have disabled",
                    " - Use of the FAWE API will not be effected"
            })
            @Default("true")
            boolean enableForConsole,
            @Comment({
                    "Should redo information be stored:",
                    " - History is about 20% larger",
                    " - Enables use of /redo"
            })
            @Default("true")
            boolean storeRedo,
            @Comment({
                    "Assumes all edits are smaller than 4096x256x4096:",
                    " - Reduces history size by ~10%"
            })
            @Default("false")
            boolean smallEdits
    ) {

    }

    @Comment("This relates to how FAWE places chunks")
    public record Queue(
            Progress progress,
            @Comment("This should equal the number of processors you have")
            // Depends on where FAWE runs, see Settings#defaults()
            int parallelThreads,
            @Comment({
                    "When doing edits that effect more than this many chunks:",
                    " - FAWE will start placing before all calculations are finished",
                    " - A larger value will use slightly less CPU time",
                    " - A smaller value will reduce memory usage",
                    " - A value too small may break some operations (deform?)",
                    " - Values smaller than the configured parallel-threads are not accepted",
                    " - It is recommended this option be at least 4x greater than parallel-threads"
            })
            // Depends on where FAWE runs, see Settings#defaults()
            int targetSize,
            @Comment({
                    "Increase or decrease queue intensity (ms) [-50,50]:",
                    "    0 = balance of performance / stability",
                    "    -10 = Allocate 10ms less for chunk placement",
                    "Too high can cause lag spikes (you might be okay with this)",
                    "Too low will result in slow edits"
            })
            @Default("0")
            int extraTimeMs,
            @Comment({
                    "Loading the right amount of chunks beforehand can speed up operations",
                    " - Low values may result in FAWE waiting on requests to the main thread",
                    " - Higher values use more memory and isn't noticeably faster",
                    " - A good (relatively) safe way to set this is",
                    " - Use 128 x GB of RAM / number of players expected to be using WE at the same time",
                    " - Paper and derivatives only. (requires delay-chunk-unloads-by to be set)."
            })
            @Default("512")
            int preloadChunkCount,
            @Comment({
                    "If pooling is enabled (reduces GC, higher memory usage)",
                    " - Enable to improve performance at the expense of memory"
            })
            @Default("true")
            boolean pool,
            @Comment({
                    "If chunk loading for writing edits to the world should be performed asynchronously to FAWE",
                    " - Enable to improve performance at the expense of memory",
                    " - If experience out of memory crashed, disable this or reduce slower-memory-percent"
            })
            @Default("true")
            boolean asyncChunkLoadWrite,
            @Comment({
                    "Percentage of queue.target-size to use per thread in multi-threaded operations",
                    " - Minimum of 100 / queue.parallel-threads (queue.target-size split across threads)",
                    " - Maximum of 100 (queue.target-size per thread)",
                    " - Higher performance at the expense of memory",
                    " - I.e. target-size=400, parallel-threads=8 and threads-target-size=25 means target-size of 100 per thread",
                    " - Defaults to 100 * 2 / parallel-threads"
            })
            // Depends on where FAWE runs, see Settings#defaults()
            int threadTargetSizePercent
    ) {

        public Queue {
            // Values smaller than the configured parallel-threads are not accepted
            targetSize = Math.max(targetSize, parallelThreads);
        }

        private static int threadTargetSizePercent(int parallelThreads) {
            return 100 * 2 / parallelThreads;
        }

        public record Progress(
                @Comment({
                        "Display constant titles about the progress of a user's edit",
                        " - false = disabled",
                        " - title = Display progress titles",
                        " - chat = Display progress in chat",
                        " - Currently not implemented"
                })
                @Default("false")
                String display,
                @Comment("How often edit progress is displayed")
                @Default("1")
                int interval,
                @Comment("Delay sending progress in milliseconds (so quick edits don't spam)")
                @Default("5000")
                int delay
        ) {

        }

    }

    @Comment({
            "Experimental options, use at your own risk",
            " - UNSAFE = Can cause permanent damage to the server",
            " - SAFE = Can be buggy but unlikely to cause any damage"
    })
    public record Experimental(
            @Comment({
                    "Undo operation batch size",
                    " - The size defines the number of changes read at once.",
                    " - Larger numbers might reduce overhead but increase latency for edits with only few changes.",
                    " - 0 means undo operations are not batched."
            })
            @Default("128")
            int undoBatchSize,
            @Comment({
                    "[SAFE] Dynamically increase the number of chunks rendered",
                    " - Set your server view distance to 1 (spigot.yml, server.properties)",
                    " - Based on TPS and player movement",
                    " - Note: If entities become hidden, increase the server view distance to 3"
            })
            @Default("-1")
            int dynamicChunkRendering,
            @Comment("Allows brushes to be persistent (default: true)")
            @Default("true")
            boolean persistentBrushes,
            @Comment({
                    "[SAFE] Keep entities that are positioned in non-air blocks when editing an area (default: true)",
                    " - Might cause client-side FPS lag in some situations",
                    " - Requires fast-placement to be true"
            })
            @Default("true")
            boolean keepEntitiesInBlocks,
            @Comment({
                    "[SAFE] Attempt to remove entities from the world if they were not present in the expected chunk (default: true)",
                    " - Sometimes an entity may have moved into a different chunk to that which FAWE expected",
                    " - This option allows FAWE to attempt to remove the entity, even if present in a different chunk",
                    " - If the entity is in an unloaded or partially loaded chunk, this will fail",
                    " - If an entity cannot be removed, it is possible duplicate entities may be created when using undo and/or redo"
            })
            @Default("true")
            boolean removeEntityFromWorldOnChunkFail,
            @Comment({
                    "[SAFE] Perform operations involving entities on chunk load",
                    " - Allows entities that might not otherwise be captured due to unloaded chunks to be captured",
                    " - Main use-case is copying larger areas with entities"
            })
            @Default("true")
            boolean improvedEntityEdits,
            @Comment("Increased debug logging for brush actions and processor setup")
            @Default("false")
            boolean other,
            @Comment({
                    "Allow fluids placed by FAWE to tick (flow). This could cause the big lags.",
                    "This has no effect on existing blocks one way or the other.",
                    "Changes due to fluid flow will not be tracked by history, thus may have unintended consequences"
            })
            @Default("false")
            boolean allowTickFluids,
            @Comment("Whether FAWE should use the incubator Vector API to accelerate some operations")
            @Default("false")
            boolean useVectorApi
    ) {

    }

    @Comment("Web/HTTP connection related settings")
    public record Web(
            @Comment({
                    "The web interface for clipboards",
                    " - All schematics are anonymous and private",
                    " - Downloads can be deleted by the user",
                    " - Supports clipboard uploads, downloads and saves"
            })
            @Default("https://schem.intellectualsites.com/fawe/")
            String url,
            @Comment("The url of the backend server (Arkitektonika)")
            @Default("https://api.schematic.cloud/")
            String arkitektonikaBackendUrl,
            @Comment({
                    "The url used to generate a download link from.",
                    "{key} will be replaced with the generated key"
            })
            @Default("https://schematic.cloud/download/{key}")
            String arkitektonikaDownloadUrl,
            @Comment({
                    "The url used to generate a deletion link from.",
                    "{key} will be replaced with the generated key"
            })
            @Default("https://schematic.cloud/delete/{key}")
            String arkitektonikaDeleteUrl,
            @Comment("The maximum amount of time in seconds the plugin can attempt to load images for.")
            @Default("5")
            int maxImageLoadTime,
            @Comment({
                    "The maximum size (width x length) an image being loaded can be.",
                    " - 8294400 is 3840x2160"
            })
            @Default("8294400")
            int maxImageSize,
            @Comment({
                    "Whitelist of hostnames to allow images to be downloaded from",
                    " - Adding '*' to the list will allow any host, but this is NOT advisable",
                    " - Crash exploits exist with malformed images",
                    " - See: https://medium.com/chargebee-engineering/perils-of-parsing-pixel-flood-attack-on-java-imageio-a97aeb06637d"
            })
            @Default("i.imgur.com")
            List<String> allowedImageHosts
    ) {

        public Web {
            allowedImageHosts = List.copyOf(allowedImageHosts);
        }

    }

    public record Extent(
            @Comment({
                    "Don't bug console when these plugins slow down WorldEdit operations",
                    " - You'll see a message in console or ingame if you need to change this option"
            })
            @Default("com.example.ExamplePlugin")
            List<String> allowedPlugins,
            @Comment("Should debug messages be sent when third party extents are used?")
            @Default("true")
            boolean debug
    ) {

        public Extent {
            allowedPlugins = List.copyOf(allowedPlugins);
        }

    }

    /**
     * @deprecated FAWE is not necessarily the tool you want to use to limit certain tick actions, e.g. fireworks or elytra flying.
     *         The code is untouched since the 1.12 era and there is no guarantee that it will work or will be maintained in the future.
     */
    @Deprecated(since = "2.0.0")
    @Comment("Generic tick limiter (not necessarily WorldEdit related, but useful to stop abuse)")
    public record TickLimiter(
            @Comment("Enable the limiter")
            @Default("false")
            boolean enabled,
            @Comment("The interval in ticks")
            @Default("20")
            int interval,
            @Comment("Max falling blocks per interval (per chunk)")
            @Default("64")
            int falling,
            @Comment("Max physics per interval (excluding redstone)")
            @Default("10")
            int physicsMs,
            @Comment("Max item spawns per interval (per chunk)")
            @Default("256")
            int items
    ) {

    }

    public record Clipboard(
            @Comment({
                    "Store the clipboard on disk instead of memory",
                    " - Will be slightly slower",
                    " - Uses 2 bytes per block"
            })
            @Default("true")
            boolean useDisk,
            @Comment({
                    "Compress the clipboard to reduce the size:",
                    " - TODO: Buffered random access with compression is not implemented on disk yet",
                    " - 0 = No compression",
                    " - 1 = Fast compression",
                    " - 2-17 = Slower compression",
                    " - levels over 6 require ZSTD 1.4.8+ to be installed to the system"
            })
            @Default("1")
            int compressionLevel,
            @Comment("Number of days to keep history on disk before deleting it")
            @Default("1")
            int deleteAfterDays,
            @Comment("If a player's clipboard should be deleted upon logout")
            @Default("false")
            boolean deleteOnLogout,
            @Comment({
                    "Allows NBT stored in a clipboard to be written to disk",
                    " - Requires clipboard.use-disk to be enabled"
            })
            @Default("true")
            boolean saveClipboardNbtToDisk,
            @Comment({
                    "Apply a file lock on the clipboard file (only relevant if clipboad.on-disk is enabled)",
                    " - Prevents other processes using the file whilst in use by FAWE",
                    " - This extends to other servers, useful if you have multiple servers using a unified clipboard folder",
                    " - May run into issues where a file lock is not correctly lifted"
            })
            @Default("false")
            boolean lockClipboardFile
    ) {

    }

    public record Lighting(
            @Comment("If packet sending should be delayed until relight is finished")
            @Default("true")
            boolean delayPacketSending,
            @Default("true")
            boolean async,
            @Comment({
                    "The relighting mode to use:",
                    " - 0 = None (Do no relighting)",
                    " - 1 = Optimal (Relight changed light sources and changed blocks)",
                    " - 2 = All (Slowly relight every blocks)"
            })
            @Default("1")
            int mode,
            @Comment("If existing lighting should be removed before relighting")
            @Default("true")
            boolean removeFirst
    ) {

    }

    public record General(
            @Comment("If the player should be relocated/unstuck when a generation command would bury them")
            @Default("true")
            boolean unstuckOnGenerate,
            @Comment(
                    "If unlimited limits should still require /confirm on large. Defaults to limits.default.confirm-large otherwise."
            )
            @Default("true")
            boolean limitUnlimitedConfirms,
            @Comment(
                    "If unlimited limits should skip sending creature spawn events. Using //fast will still disable spawn events."
            )
            @Default("true")
            boolean limitUnlimitedSkipsCreatureSpawnEvents
    ) {

    }

}
