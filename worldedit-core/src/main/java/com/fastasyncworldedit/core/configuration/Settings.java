package com.fastasyncworldedit.core.configuration;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.limit.PropertyRemap;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Settings extends Config {

    @Ignore
    static Settings INSTANCE = new Settings();
    /**
     * @deprecated Use {@link #settings()} instead to get an instance.
     */
    @Ignore
    @Deprecated(forRemoval = true, since = "2.0.0")
    public static final Settings IMP = INSTANCE;
    @Ignore
    public boolean PROTOCOL_SUPPORT_FIX = false;
    @Comment("These first 6 aren't configurable") // This is a comment
    @Final // Indicates that this value isn't configurable
    @SuppressWarnings("unused")
    public String ISSUES = "https://github.com/IntellectualSites/FastAsyncWorldEdit/issues";
    @Final
    @SuppressWarnings("unused")
    public String WIKI = "https://intellectualsites.github.io/fastasyncworldedit-documentation/";
    @Final
    public String DATE; // These values are set from FAWE before loading
    @Final
    public String BUILD; // These values are set from FAWE before loading
    @Final
    public String COMMIT; // These values are set from FAWE before loading
    @Final
    public String PLATFORM; // These values are set from FAWE before loading
    @Comment({
            "Set true to enable WorldEdit restrictions per region (e.g. PlotSquared or WorldGuard).",
            "To be allowed to WorldEdit in a region, users need the appropriate",
            "fawe.<plugin>  permission. See the Permissions page for supported region plugins."
    })
    public boolean REGION_RESTRICTIONS = true;
    @Comment({
            "FAWE will cancel non admin edits when memory consumption exceeds this %",
            " - Bypass with `/wea` or `//fast` or `fawe.bypass`",
            " - Disable with 100 or -1."
    })
    public int MAX_MEMORY_PERCENT = 95;
    @Create
    public ENABLED_COMPONENTS ENABLED_COMPONENTS;
    @Create
    public CLIPBOARD CLIPBOARD;
    @Create
    public LIGHTING LIGHTING;
    @Create
    public TICK_LIMITER TICK_LIMITER;
    @Create
    public WEB WEB;
    @Create
    public EXTENT EXTENT;
    @Create
    public EXPERIMENTAL EXPERIMENTAL;
    @Create
    public QUEUE QUEUE;
    @Create
    public HISTORY HISTORY;
    @Create
    public PATHS PATHS;
    @Create
    public REGION_RESTRICTIONS_OPTIONS REGION_RESTRICTIONS_OPTIONS;
    @Create
    public GENERAL GENERAL;
    @Create
    public ConfigBlock<LIMITS> LIMITS;

    private Settings() {
        INSTANCE = this;
    }

    /**
     * Gets an instance of Settings.
     *
     * @return an instance of Settings
     * @since 2.0.0
     */
    public static Settings settings() {
        return INSTANCE;
    }

    public void reload(File file) {
        load(file);
        save(file);
    }

    public FaweLimit getLimit(Actor actor) {
        FaweLimit limit;
        if (actor.hasPermission("fawe.limit.unlimited")) {
            return FaweLimit.MAX.copy();
        }
        limit = new FaweLimit();
        ArrayList<String> keys = new ArrayList<>(LIMITS.getSections());
        if (keys.remove("default")) {
            keys.add("default");
        }

        boolean limitFound = false;
        for (String key : keys) {
            if (actor.hasPermission("fawe.limit." + key) || !limitFound && key.equals("default")) {
                limitFound = true;
                LIMITS newLimit = LIMITS.get(key);
                limit.MAX_ACTIONS = Math.max(
                        limit.MAX_ACTIONS,
                        newLimit.MAX_ACTIONS != -1 ? newLimit.MAX_ACTIONS : Integer.MAX_VALUE
                );
                limit.MAX_CHANGES = Math.max(
                        limit.MAX_CHANGES,
                        newLimit.MAX_CHANGES != -1 ? newLimit.MAX_CHANGES : Long.MAX_VALUE
                );
                limit.MAX_BLOCKSTATES = Math.max(
                        limit.MAX_BLOCKSTATES,
                        newLimit.MAX_BLOCKSTATES != -1 ? newLimit.MAX_BLOCKSTATES : Integer.MAX_VALUE
                );
                limit.MAX_CHECKS = Math.max(
                        limit.MAX_CHECKS,
                        newLimit.MAX_CHECKS != -1 ? newLimit.MAX_CHECKS : Long.MAX_VALUE
                );
                limit.MAX_ENTITIES = Math.max(
                        limit.MAX_ENTITIES,
                        newLimit.MAX_ENTITIES != -1 ? newLimit.MAX_ENTITIES : Integer.MAX_VALUE
                );
                limit.MAX_FAILS = Math.max(limit.MAX_FAILS, newLimit.MAX_FAILS != -1 ? newLimit.MAX_FAILS : Integer.MAX_VALUE);
                limit.MAX_ITERATIONS = Math.max(
                        limit.MAX_ITERATIONS, newLimit.MAX_ITERATIONS != -1 ? newLimit.MAX_ITERATIONS : Integer.MAX_VALUE);
                limit.MAX_RADIUS = Math.max(
                        limit.MAX_RADIUS,
                        newLimit.MAX_RADIUS != -1 ? newLimit.MAX_RADIUS : Integer.MAX_VALUE
                );
                limit.MAX_SUPER_PICKAXE_SIZE = Math.max(
                        limit.MAX_SUPER_PICKAXE_SIZE,
                        newLimit.MAX_SUPER_PICKAXE_SIZE != -1 ? newLimit.MAX_SUPER_PICKAXE_SIZE : Integer.MAX_VALUE
                );
                limit.MAX_BRUSH_RADIUS = Math.max(
                        limit.MAX_BRUSH_RADIUS,
                        newLimit.MAX_BRUSH_RADIUS != -1 ? newLimit.MAX_BRUSH_RADIUS : Integer.MAX_VALUE
                );
                limit.MAX_BUTCHER_RADIUS = Math.max(
                        limit.MAX_BUTCHER_RADIUS,
                        newLimit.MAX_BUTCHER_RADIUS != -1 ? newLimit.MAX_BUTCHER_RADIUS : Integer.MAX_VALUE
                );
                limit.MAX_HISTORY = Math.max(
                        limit.MAX_HISTORY,
                        newLimit.MAX_HISTORY_MB != -1 ? newLimit.MAX_HISTORY_MB : Integer.MAX_VALUE
                );
                limit.SCHEM_FILE_NUM_LIMIT = Math.max(
                        limit.SCHEM_FILE_NUM_LIMIT,
                        newLimit.SCHEM_FILE_NUM_LIMIT != -1 ? newLimit.SCHEM_FILE_NUM_LIMIT : Integer.MAX_VALUE
                );
                limit.SCHEM_FILE_SIZE_LIMIT = Math.max(
                        limit.SCHEM_FILE_SIZE_LIMIT,
                        newLimit.SCHEM_FILE_SIZE_LIMIT != -1 ? newLimit.SCHEM_FILE_SIZE_LIMIT : Integer.MAX_VALUE
                );
                limit.MAX_EXPRESSION_MS = Math.max(
                        limit.MAX_EXPRESSION_MS,
                        newLimit.MAX_EXPRESSION_MS != -1 ? newLimit.MAX_EXPRESSION_MS : Integer.MAX_VALUE
                );
                limit.INVENTORY_MODE = Math.min(limit.INVENTORY_MODE, newLimit.INVENTORY_MODE);
                limit.SPEED_REDUCTION = Math.min(limit.SPEED_REDUCTION, newLimit.SPEED_REDUCTION);
                limit.FAST_PLACEMENT |= newLimit.FAST_PLACEMENT;
                limit.CONFIRM_LARGE &= newLimit.CONFIRM_LARGE;
                limit.RESTRICT_HISTORY_TO_REGIONS &= newLimit.RESTRICT_HISTORY_TO_REGIONS;
                if (limit.STRIP_NBT == null) {
                    limit.STRIP_NBT = newLimit.STRIP_NBT.isEmpty() ? Collections.emptySet() : new HashSet<>(newLimit.STRIP_NBT);
                } else if (limit.STRIP_NBT.isEmpty() || newLimit.STRIP_NBT.isEmpty()) {
                    limit.STRIP_NBT = Collections.emptySet();
                } else {
                    limit.STRIP_NBT = new HashSet<>(limit.STRIP_NBT);
                    limit.STRIP_NBT.retainAll(newLimit.STRIP_NBT);
                    if (limit.STRIP_NBT.isEmpty()) {
                        limit.STRIP_NBT = Collections.emptySet();
                    }
                }
                limit.UNIVERSAL_DISALLOWED_BLOCKS &= newLimit.UNIVERSAL_DISALLOWED_BLOCKS;

                if (limit.DISALLOWED_BLOCKS == null) {
                    limit.DISALLOWED_BLOCKS = newLimit.DISALLOWED_BLOCKS.isEmpty() ? Collections.emptySet() : new HashSet<>(
                            newLimit.DISALLOWED_BLOCKS);
                } else if (limit.DISALLOWED_BLOCKS.isEmpty() || newLimit.DISALLOWED_BLOCKS.isEmpty()) {
                    limit.DISALLOWED_BLOCKS = Collections.emptySet();
                } else {
                    limit.DISALLOWED_BLOCKS = new HashSet<>(limit.DISALLOWED_BLOCKS);
                    limit.DISALLOWED_BLOCKS.retainAll(newLimit.DISALLOWED_BLOCKS
                            .stream()
                            .map(s -> s.contains(":") ? s.toLowerCase(Locale.ROOT) : ("minecraft:" + s).toLowerCase(Locale.ROOT))
                            .collect(Collectors.toSet()));
                    if (limit.DISALLOWED_BLOCKS.isEmpty()) {
                        limit.DISALLOWED_BLOCKS = Collections.emptySet();
                    }
                }

                if (limit.REMAP_PROPERTIES == null) {
                    limit.REMAP_PROPERTIES = newLimit.REMAP_PROPERTIES.isEmpty() ? Collections.emptySet() :
                            newLimit.REMAP_PROPERTIES.stream().flatMap(s -> {
                                String propertyStr = s.substring(0, s.indexOf('['));
                                List<Property<?>> properties =
                                        BlockTypesCache.getAllProperties().get(propertyStr.toLowerCase(Locale.ROOT));
                                if (properties == null || properties.isEmpty()) {
                                    return Stream.empty();
                                }
                                String[] mappings = s.substring(s.indexOf('[') + 1, s.indexOf(']')).split(",");
                                Set<PropertyRemap<?>> remaps = new HashSet<>();
                                for (Property<?> property : properties) {
                                    for (String mapping : mappings) {
                                        try {
                                            String[] fromTo = mapping.split(":");
                                            remaps.add(property.getRemap(
                                                    property.getValueFor(fromTo[0]),
                                                    property.getValueFor(fromTo[1])
                                            ));
                                        } catch (IllegalArgumentException ignored) {
                                            // This property is unlikely to be the one being targeted.
                                            break;
                                        }
                                    }
                                }
                                return remaps.stream();
                            }).collect(Collectors.toSet());
                } else if (limit.REMAP_PROPERTIES.isEmpty() || newLimit.REMAP_PROPERTIES.isEmpty()) {
                    limit.REMAP_PROPERTIES = Collections.emptySet();
                } else {
                    limit.REMAP_PROPERTIES = new HashSet<>(limit.REMAP_PROPERTIES);
                    limit.REMAP_PROPERTIES.retainAll(newLimit.REMAP_PROPERTIES.stream().flatMap(s -> {
                        String propertyStr = s.substring(0, s.indexOf('['));
                        List<Property<?>> properties =
                                BlockTypesCache.getAllProperties().get(propertyStr.toLowerCase(Locale.ROOT));
                        if (properties == null || properties.isEmpty()) {
                            return Stream.empty();
                        }
                        String[] mappings = s.substring(s.indexOf('[') + 1, s.indexOf(']')).split(",");
                        Set<PropertyRemap<?>> remaps = new HashSet<>();
                        for (Property<?> property : properties) {
                            for (String mapping : mappings) {
                                try {
                                    String[] fromTo = mapping.split(":");
                                    remaps.add(property.getRemap(
                                            property.getValueFor(fromTo[0]),
                                            property.getValueFor(fromTo[1])
                                    ));
                                } catch (IllegalArgumentException ignored) {
                                    // This property is unlikely to be the one being targeted.
                                    break;
                                }
                            }
                        }
                        return remaps.stream();
                    }).collect(Collectors.toSet()));
                    if (limit.REMAP_PROPERTIES.isEmpty()) {
                        limit.REMAP_PROPERTIES = Collections.emptySet();
                    }
                }
            }
        }
        return limit;
    }

    @Comment("Enable or disable core components")
    public static final class ENABLED_COMPONENTS {

        public boolean COMMANDS = true;
        @Comment({"Show additional information in console. It helps us at IntellectualSites to find out more about an issue.",
                "Leave it off if you don't need it, it can spam your console."})
        public boolean DEBUG = false;
        @Comment({"Whether or not FAWE should notify you on startup about new versions available."})
        public boolean UPDATE_NOTIFICATIONS = true;

    }

    @Comment("Paths for various directories")
    public static final class PATHS {

        @Comment({
                "Put any minecraft or mod jars for FAWE to be aware of block textures",
        })
        public String TEXTURES = "textures";
        public String HEIGHTMAP = "heightmap";
        public String HISTORY = "history";
        @Comment({
                "Multiple servers can use the same clipboards",
                " - Use a shared directory or NFS/Samba"
        })
        public String CLIPBOARD = "clipboard";
        @Comment("Each player has his or her own sub directory for schematics")
        public boolean PER_PLAYER_SCHEMATICS = false;

    }

    @Comment("Region restriction settings")
    public static final class REGION_RESTRICTIONS_OPTIONS {

        @Comment({
                "What type of users are allowed to WorldEdit in a region",
                " - MEMBER = Players added to a region",
                " - OWNER = Players who own the region"
        })
        public String MODE = "MEMBER";
        @Comment({
                "Allow region blacklists.",
                " - Currently only implemented for WorldGuard",
                " - see region-restrictions-options.worldguard-region-blacklist"
        })
        public boolean ALLOW_BLACKLISTS = false;
        @Comment({
                "List of plugin mask managers that should be exclusive. Exclusive managers are not ",
                "checked for edit restrictions if another manager already allowed an edit, and further ",
                "managers are not checked if an exclusive manager allows an edit.",
                " - May be useful to add PlotSquared if using both P2 and WorldGuard on a server",
                " - Some custom-implementations in other plugins may override this setting"
        })
        public List<String> EXCLUSIVE_MANAGERS = new ArrayList<>(Collections.singleton(("ExamplePlugin")));
        @Comment({
                "If a worldguard-protected world should be considered as a region blacklist.",
                " - This will create a blacklist of regions where an edit cannot operate.",
                " - Useful for a \"freebuild\" worlds with few protected areas.",
                " - May cause performance loss with large numbers of protected areas.",
                " - Requires region-restrictions-options.allow-blacklists be true.",
                " - Will still search for current allowed regions to limit the edit to.",
                " - Any blacklist regions are likely to override any internal allowed regions."
        })
        public boolean WORLDGUARD_REGION_BLACKLIST = false;
        @Comment({
                "Restrict all edits to within the safe chunk limits of +/- 30 million blocks",
                " - Edits outside this range may induce crashing",
                " - Forcefully prevents any edit outside this range"
        })
        public boolean RESTRICT_TO_SAFE_RANGE = true;

    }

    @Comment({
            "The \"default\" limit group affects those without a specific limit permission.",
            "To grant someone different limits, copy the default limits group",
            "and give it a different name (e.g. newbie). Then give the user the limit ",
            "permission node with that limit name (e.g. fawe.limit.newbie  )"
    })
    @BlockName("default") // The name for the default block
    public static class LIMITS extends ConfigBlock {

        @Comment("Max actions that can be run concurrently (i.e. commands)")
        public int MAX_ACTIONS = 1;
        @Comment("Max number of block changes (e.g. by `//set stone`).")
        public long MAX_CHANGES = 50000000;
        @Comment("Max number of blocks checked (e.g. `//count stone` which doesn't change blocks)")
        public long MAX_CHECKS = 50000000;
        @Comment("Number of times a change can fail (e.g. if the player can't access that region)")
        public int MAX_FAILS = 50000000;
        @Comment("Allowed brush iterations (e.g. `//brush smooth`)")
        public int MAX_ITERATIONS = 1000;
        @Comment("Max allowed entities (e.g. cows)")
        public int MAX_ENTITIES = 1337;
        @Comment("Max allowed radius (e.g. for //sphere)")
        public int MAX_RADIUS = LocalConfiguration.MAX_RADIUS;
        @Comment("Max allowed superpickaxe size")
        public int MAX_SUPER_PICKAXE_SIZE = LocalConfiguration.MAX_SUPER_RADIUS;
        @Comment("Max allowed brush radius")
        public int MAX_BRUSH_RADIUS = LocalConfiguration.MAX_BRUSH_RADIUS;
        @Comment("Max allowed butcher radius")
        public int MAX_BUTCHER_RADIUS = LocalConfiguration.MAX_BUTCHER_RADIUS;
        @Comment({
                "Blockstates include Banner, Beacon, BrewingStand, Chest, CommandBlock, ",
                "CreatureSpawner, Dispenser, Dropper, EndGateway, Furnace, Hopper, Jukebox, ",
                "NoteBlock, Sign, Skull, Structure"
        })
        public int MAX_BLOCKSTATES = 1337;
        @Comment({
                "Maximum size of the player's history in Megabytes:",
                " - History on disk or memory will be deleted",
        })
        public int MAX_HISTORY_MB = -1;
        @Comment({
                "Sets a maximum limit (in kb) for the size of a player's schematics directory (per-player mode only)",
                "Set to -1 to disable"
        })
        @Migrate("experimental.per-player-file-size-limit")
        public int SCHEM_FILE_SIZE_LIMIT = -1;
        @Comment({
                "Sets a maximum limit for the amount of schematics in a player's schematics directory (per-player mode only)",
                "Set to -1 to disable"
        })
        @Migrate("experimental.per-player-file-num-limit")
        public int SCHEM_FILE_NUM_LIMIT = -1;
        @Comment("Maximum time in milliseconds //calc can execute")
        public int MAX_EXPRESSION_MS = 50;
        @Comment({
                "Cinematic block placement:",
                " - Adds a delay to block placement (nanoseconds/block)",
                " - Having an artificial delay will use more CPU/Memory",
        })
        public int SPEED_REDUCTION = 0;
        @Comment({
                "Place chunks instead of individual blocks:",
                " - Disabling this will negatively impact performance",
                " - Only disable this for compatibility or cinematic placement",
        })
        public boolean FAST_PLACEMENT = true;
        @Comment({
                "Should WorldEdit use inventory?",
                "0 = No inventory usage (creative)",
                "1 = Inventory for removing and placing (freebuild)",
                "2 = Inventory for placing (survival)",
        })
        public int INVENTORY_MODE = 0;
        @Comment({
                "Should large edits require confirmation (>16384 chunks)",
        })
        public boolean CONFIRM_LARGE = true;
        @Comment({
                "If undo and redo commands should be restricted to allowed regions",
                " - Prevents scenarios where players can delete/reset a region, and then continue to undo/redo on it"
        })
        public boolean RESTRICT_HISTORY_TO_REGIONS = true;
        @Comment({
                "List of nbt tags to strip from blocks, e.g. Items",
        })
        public List<String> STRIP_NBT = new ArrayList<>();
        @Comment({
                "If the disallowed blocks listed in worldedit-config.yml should be disallowed in all edits,",
                "not just where blocks patterns are used.",
                " - Can prevent blocks being pasted from clipboards, etc.",
                " - If fast-placement is disabled, this may cause edits to be slower."
        })
        public boolean UNIVERSAL_DISALLOWED_BLOCKS = true;
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
        public List<String> DISALLOWED_BLOCKS = Arrays.asList("minecraft:wheat", "minecraft:fire", "minecraft:redstone_wire");
        @Comment({
                "List of block properties that should be remapped if used in an edit. Entries should take the form",
                "\"property_name[value1_old:value1_new,value2_old:value2_new]\". For example:",
                " - \"waterlogged[true:false]\"",
                " - \"age[7:4,6:4,5:4]\"",
                " - \"extended[true:false]\""
        })
        public List<String> REMAP_PROPERTIES = new ArrayList<>();

    }

    public static class HISTORY {

        @Comment({
                "Should history be saved on disk:",
                " - Frees up a lot of memory",
                " - Persists restarts",
                " - Unlimited undo",
                " - Does not affect edit performance if `combine-stages`",
        })
        public boolean USE_DISK = true;
        @Comment({
                "Use a database to store disk storage summaries:",
                " - Enables inspection and rollback",
                " - Does not impact performance",
        })
        public boolean USE_DATABASE = true;
        @Comment({
                "Record history with dispatching:",
                " - Much faster as it avoids duplicate block checks",
                " - Slightly worse compression since dispatch order is different",
        })
        public boolean COMBINE_STAGES = true;
        @Comment({
                "Do not wait for a chunk's history to save before sending it",
                " - Undo/redo commands will wait until the history has been written to disk before executing",
                " - Requires combine-stages = true"
        })
        public boolean SEND_BEFORE_HISTORY = true;
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
        public int COMPRESSION_LEVEL = 3;
        @Comment({
                "The buffer size for compression:",
                " - Larger = better ratio but uses more upfront memory",
                " - Must be in the range [64, 33554432]",
        })
        public int BUFFER_SIZE = 531441;

        @Comment("Delete history on disk after a number of days")
        public int DELETE_AFTER_DAYS = 7;
        @Comment("Delete history in memory on logout (does not effect disk)")
        public boolean DELETE_ON_LOGOUT = true;
        @Comment({
                "If history should be enabled by default for plugins using WorldEdit:",
                " - It is faster to have disabled",
                " - It is faster to have disabled",
                " - Use of the FAWE API will not be effected"
        })
        public boolean ENABLE_FOR_CONSOLE = true;
        @Comment({
                "Should redo information be stored:",
                " - History is about 20% larger",
                " - Enables use of /redo",
        })
        public boolean STORE_REDO = true;
        @Comment({
                "Assumes all edits are smaller than 4096x256x4096:",
                " - Reduces history size by ~10%",
        })
        public boolean SMALL_EDITS = false;

    }

    @Comment("This relates to how FAWE places chunks")
    public static class QUEUE {

        @Create
        public static PROGRESS PROGRESS;

        @Comment({
                "This should equal the number of processors you have",
        })
        public int PARALLEL_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());

        @Comment({
                "When doing edits that effect more than this many chunks:",
                " - FAWE will start placing before all calculations are finished",
                " - A larger value will use slightly less CPU time",
                " - A smaller value will reduce memory usage",
                " - A value too small may break some operations (deform?)",
                " - Values smaller than the configured parallel-threads are not accepted",
                " - It is recommended this option be at least 4x greater than parallel-threads"

        })
        public int TARGET_SIZE = 8 * Runtime.getRuntime().availableProcessors();

        @Comment({
                "Increase or decrease queue intensity (ms) [-50,50]:",
                "    0 = balance of performance / stability",
                "    -10 = Allocate 10ms less for chunk placement",
                "Too high can cause lag spikes (you might be okay with this)",
                "Too low will result in slow edits",
        })
        public int EXTRA_TIME_MS = 0;

        @Comment({
                "Loading the right amount of chunks beforehand can speed up operations",
                " - Low values may result in FAWE waiting on requests to the main thread",
                " - Higher values use more memory and isn't noticeably faster",
                " - A good (relatively) safe way to set this is",
                " - Use 128 x GB of RAM / number of players expected to be using WE at the same time",
                " - Paper and derivatives only. (requires delay-chunk-unloads-by to be set)."
        })
        // Renamed from PRELOAD_CHUNK because it was set to 100000... something that lots of servers will now have which is
        // wayyy too much...
        public int PRELOAD_CHUNK_COUNT = 512;

        @Comment({
                "If pooling is enabled (reduces GC, higher memory usage)",
                " - Enable to improve performance at the expense of memory",
        })
        public boolean POOL = true;

        public static class PROGRESS {

            @Comment({"Display constant titles about the progress of a user's edit",
                    " - false = disabled",
                    " - title = Display progress titles",
                    " - chat = Display progress in chat",
                    " - Currently not implemented"
            })
            public String DISPLAY = "false";
            @Comment("How often edit progress is displayed")
            public int INTERVAL = 1;
            @Comment("Delay sending progress in milliseconds (so quick edits don't spam)")
            public int DELAY = 5000;

        }

    }

    @Comment({
            "Experimental options, use at your own risk",
            " - UNSAFE = Can cause permanent damage to the server",
            " - SAFE = Can be buggy but unlikely to cause any damage"
    })
    public static class EXPERIMENTAL {

        @Comment({
                "[UNSAFE] Directly modify the region files. (OBSOLETE - USE ANVIL COMMANDS)",
                " - IMPROPER USE CAN CAUSE WORLD CORRUPTION!",
        })
        public boolean ANVIL_QUEUE_MODE = false;
        @Comment({
                "[SAFE] Dynamically increase the number of chunks rendered",
                " - Requires Paper",
                " - Set your server view distance to 1 (spigot.yml, server.properties)",
                " - Based on tps and player movement",
                " - Note: If entities become hidden, increase the server view distance to 3",
        })
        public int DYNAMIC_CHUNK_RENDERING = -1;
        @Comment({
                "Allows brushes to be persistent (default: true)",
        })
        public boolean PERSISTENT_BRUSHES = true;

        @Comment({
                "[SAFE] Keep entities that are positioned in non-air blocks when editing an area (default: true)",
                " - Might cause client-side FPS lag in some situations",
                " - Requires fast-placement to be true"
        })
        public boolean KEEP_ENTITIES_IN_BLOCKS = true;

        @Comment({
                "[SAFE] Attempt to remove entities from the world if they were not present in the expected chunk (default: true)",
                " - Sometimes an entity may have moved into a different chunk to that which FAWE expected",
                " - This option allows FAWE to attempt to remove the entity, even if present in a different chunk",
                " - If the entity is in an unloaded or partially loaded chunk, this will fail",
                " - If an entity cannot be removed, it is possible duplicate entities may be created when using undo and/or redo"
        })
        public boolean REMOVE_ENTITY_FROM_WORLD_ON_CHUNK_FAIL = true;

        @Comment({
                "Increased debug logging for brush actions and processor setup"
        })
        public boolean OTHER = false;

        @Comment({
                "Allow fluids placed by FAWE to tick (flow). This could cause the big lags.",
                "This has no effect on existing blocks one way or the other.",
                "Changes due to fluid flow will not be tracked by history, thus may have unintended consequences"
        })
        public boolean ALLOW_TICK_FLUIDS = false;

    }

    @Comment({"Web/HTTP connection related settings"})
    public static class WEB {

        @Comment({"The web interface for clipboards", " - All schematics are anonymous and private", " - Downloads can be deleted by the user", " - Supports clipboard uploads, downloads and saves",})
        public String URL = "https://schem.intellectualsites.com/fawe/";

        @Comment("The maximum amount of time in seconds the plugin can attempt to load images for.")
        public int MAX_IMAGE_LOAD_TIME = 5;

        @Comment({
                "The maximum size (width x length) an image being loaded can be.",
                " - 8294400 is 3840x2160"
        })
        public int MAX_IMAGE_SIZE = 8294400;

        @Comment({
                "Whitelist of hostnames to allow images to be downloaded from",
                " - Adding '*' to the list will allow any host, but this is NOT adviseable",
                " - Crash exploits exist with malformed images",
                " - See: https://medium.com/chargebee-engineering/perils-of-parsing-pixel-flood-attack-on-java-imageio-a97aeb06637d"
        })
        public List<String> ALLOWED_IMAGE_HOSTS = new ArrayList<>(Collections.singleton(("i.imgur.com")));

    }

    public static class EXTENT {

        @Comment({
                "Don't bug console when these plugins slow down WorldEdit operations",
                " - You'll see a message in console or ingame if you need to change this option"
        })
        public List<String> ALLOWED_PLUGINS = new ArrayList<>(Collections.singleton(("com.example.ExamplePlugin")));
        @Comment("Should debug messages be sent when third party extents are used?")
        public boolean DEBUG = true;

    }

    /**
     * @deprecated FAWE is not necessarily the tool you want to use to limit certain tick actions, e.g. fireworks or elytra flying.
     * The code is untouched since the 1.12 era and there is no guarantee that it will work or will be maintained in the future.
     */
    @Deprecated(since = "2.0.0")
    @Comment("Generic tick limiter (not necessarily WorldEdit related, but useful to stop abuse)")
    public static class TICK_LIMITER {

        @Comment("Enable the limiter")
        public boolean ENABLED = false;
        @Comment("The interval in ticks")
        public int INTERVAL = 20;
        @Comment("Max falling blocks per interval (per chunk)")
        public int FALLING = 64;
        @Comment("Max physics per interval (excluding redstone)")
        public int PHYSICS_MS = 10;
        @Comment("Max item spawns per interval (per chunk)")
        public int ITEMS = 256;

    }

    public static class CLIPBOARD {

        @Comment({
                "Store the clipboard on disk instead of memory",
                " - Will be slightly slower",
                " - Uses 2 bytes per block",
        })
        public boolean USE_DISK = true;
        @Comment({
                "Compress the clipboard to reduce the size:",
                " - TODO: Buffered random access with compression is not implemented on disk yet",
                " - 0 = No compression",
                " - 1 = Fast compression",
                " - 2-17 = Slower compression",
                " - levels over 6 require ZSTD 1.4.8+ to be installed to the system"
        })
        public int COMPRESSION_LEVEL = 1;
        @Comment("Number of days to keep history on disk before deleting it")
        public int DELETE_AFTER_DAYS = 1;
        @Comment({
                "If a player's clipboard should be deleted upon logout"
        })
        public boolean DELETE_ON_LOGOUT = false;
        @Comment({
                "Allows NBT stored in a clipboard to be written to disk",
                " - Requires clipboard.use-disk to be enabled"
        })
        public boolean SAVE_CLIPBOARD_NBT_TO_DISK = true;
        @Comment({
                "Apply a file lock on the clipboard file (only relevant if clipboad.on-disk is enabled)",
                " - Prevents other processes using the file whilst in use by FAWE",
                " - This extends to other servers, useful if you have multiple servers using a unified clipboard folder",
                " - May run into issues where a file lock is not correctly lifted"
        })
        public boolean LOCK_CLIPBOARD_FILE = false;

    }

    public static class LIGHTING {

        @Comment({
                "If packet sending should be delayed until relight is finished",
        })
        public boolean DELAY_PACKET_SENDING = true;
        public boolean ASYNC = true;
        @Comment({
                "The relighting mode to use:",
                " - 0 = None (Do no relighting)",
                " - 1 = Optimal (Relight changed light sources and changed blocks)",
                " - 2 = All (Slowly relight every blocks)",
        })
        public int MODE = 1;
        @Comment({"If existing lighting should be removed before relighting"})
        public boolean REMOVE_FIRST = true;

    }

    public static class GENERAL {

        @Comment({
                "If the player should be relocated/unstuck when a generation command would bury them",
        })
        public boolean UNSTUCK_ON_GENERATE = true;

    }

}
