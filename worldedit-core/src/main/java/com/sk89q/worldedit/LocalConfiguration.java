/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.fastasyncworldedit.core.function.mask.BlockMaskBuilder;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.component.TextUtils;
import com.sk89q.worldedit.util.io.file.ArchiveNioSupports;
import com.sk89q.worldedit.util.logging.LogFormat;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.sk89q.worldedit.world.snapshot.SnapshotRepository;
import com.sk89q.worldedit.world.snapshot.experimental.SnapshotDatabase;
import com.sk89q.worldedit.world.snapshot.experimental.fs.FileSystemSnapshotDatabase;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents WorldEdit's configuration.
 */
public abstract class LocalConfiguration {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    //FAWE start - inelegant but required to transfer to FAWE limits as defaults
    public static int MAX_RADIUS;
    public static int MAX_SUPER_RADIUS;
    public static int MAX_BRUSH_RADIUS;
    public static int MAX_BUTCHER_RADIUS;
    //FAWE end

    public boolean profile = false;
    public boolean traceUnflushedSessions = true;
    public Set<String> disallowedBlocks = new HashSet<>();
    protected BlockMask disallowedBlocksMask;
    //FAWE start - the blocks the mask was built from, to notice when a reload replaces them
    private Set<String> disallowedBlocksMaskSource;
    //FAWE end
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_CHANGES}
     */
    @Deprecated
    public int defaultChangeLimit = -1;
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_CHANGES}
     */
    @Deprecated
    public int maxChangeLimit = -1;
    public int defaultVerticalHeight = 256;
    public int defaultMaxPolygonalPoints = -1;
    public int maxPolygonalPoints = 20;
    public int defaultMaxPolyhedronPoints = -1;
    public int maxPolyhedronPoints = 20;
    public String shellSaveType = "";
    public boolean snapshotsConfigured = false;
    public SnapshotRepository snapshotRepo = null;
    public SnapshotDatabase snapshotDatabase = null;
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_RADIUS}
     */
    @Deprecated
    public int maxRadius = -1;
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_SUPER_PICKAXE_SIZE}
     */
    @Deprecated
    public int maxSuperPickaxeSize = 5;
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_BRUSH_RADIUS}
     */
    @Deprecated
    public int maxBrushRadius = 6;
    public boolean logCommands = false;
    public String logFile = "";
    public String logFormat = LogFormat.DEFAULT_FORMAT;
    public boolean registerHelp = true; // unused
    public String wandItem = "minecraft:wooden_axe";
    public boolean superPickaxeDrop = true;
    public boolean superPickaxeManyDrop = true;
    public boolean useInventory = false;
    public boolean useInventoryOverride = false;
    public boolean useInventoryCreativeOverride = false;
    public boolean navigationUseGlass = true;
    public String navigationWand = "minecraft:compass";
    public int navigationWandMaxDistance = 50;
    public int scriptTimeout = 3000;
    public int calculationTimeout = 100;
    public int maxCalculationTimeout = 300;
    public Set<String> allowedDataCycleBlocks = new HashSet<>();
    public String saveDir = "schematics";
    public String scriptsDir = "craftscripts";
    public boolean showHelpInfo = true; // unused
    public int butcherDefaultRadius = -1;
    /**
     * @deprecated Use actor's limit {@link com.fastasyncworldedit.core.limit.FaweLimit#MAX_BUTCHER_RADIUS}
     */
    @Deprecated
    public int butcherMaxRadius = -1;
    public boolean allowSymlinks = false;
    public boolean serverSideCUI = true;
    public boolean extendedYLimit = false;
    public boolean commandBlockSupport = false;
    public String defaultLocaleName = "default";
    public Locale defaultLocale = Locale.getDefault();

    //FAWE Start - Remove the default blocks and change the return type to List to allow for more flexibility in the future
    protected List<String> getDefaultDisallowedBlocks() {
        return Collections.emptyList();
    }
    // FAWE End

    /**
     * Load the configuration.
     */
    public abstract void load();

    //FAWE start - declare each option once for every file format
    /**
     * Where the values of the options come from. An option is looked up under its path in a YAML configuration or, on
     * platforms configured through a properties file, under its property key.
     *
     * @since TODO
     */
    protected interface OptionSource {

        /**
         * Get the value of an option.
         *
         * @param path     the dot-separated path of the option in a YAML configuration
         * @param property the key of the option in a properties file, or null if it has none
         * @param def      the value to use if the option is not set
         * @return the value
         */
        boolean getBoolean(String path, @Nullable String property, boolean def);

        /**
         * Get the value of an option.
         *
         * @see #getBoolean(String, String, boolean)
         */
        int getInt(String path, @Nullable String property, int def);

        /**
         * Get the value of an option.
         *
         * @see #getBoolean(String, String, boolean)
         */
        String getString(String path, @Nullable String property, String def);

        /**
         * Get the value of an option.
         *
         * @see #getBoolean(String, String, boolean)
         */
        List<String> getStringList(String path, @Nullable String property, List<String> def);

    }

    /**
     * Read the options that all platforms share. An option that is not set keeps its current value.
     *
     * @param source where to read the options from
     * @since TODO
     */
    protected void loadOptions(OptionSource source) {
        profile = source.getBoolean("debug", "profile", profile);
        traceUnflushedSessions = source.getBoolean(
                "debugging.trace-unflushed-sessions",
                "trace-unflushed-sessions",
                traceUnflushedSessions
        );
        wandItem = convertLegacyItem(source.getString("wand-item", "wand-item", wandItem));

        defaultChangeLimit = Math.max(
                -1,
                source.getInt("limits.max-blocks-changed.default", "default-max-changed-blocks", defaultChangeLimit)
        );
        maxChangeLimit = Math.max(-1, source.getInt("limits.max-blocks-changed.maximum", "max-changed-blocks", maxChangeLimit));
        defaultVerticalHeight = Math.max(
                1,
                source.getInt("limits.vertical-height.default", "default-vertical-height", defaultVerticalHeight)
        );
        defaultMaxPolygonalPoints = Math.max(
                -1,
                source.getInt("limits.max-polygonal-points.default", "default-max-polygon-points", defaultMaxPolygonalPoints)
        );
        maxPolygonalPoints = Math.max(
                -1,
                source.getInt("limits.max-polygonal-points.maximum", "max-polygon-points", maxPolygonalPoints)
        );
        defaultMaxPolyhedronPoints = Math.max(
                -1,
                source.getInt("limits.max-polyhedron-points.default", "default-max-polyhedron-points", defaultMaxPolyhedronPoints)
        );
        maxPolyhedronPoints = Math.max(
                -1,
                source.getInt("limits.max-polyhedron-points.maximum", "max-polyhedron-points", maxPolyhedronPoints)
        );
        maxRadius = Math.max(-1, source.getInt("limits.max-radius", "max-radius", maxRadius));
        maxBrushRadius = source.getInt("limits.max-brush-radius", "max-brush-radius", maxBrushRadius);
        maxSuperPickaxeSize = Math.max(
                1,
                source.getInt("limits.max-super-pickaxe-size", "max-super-pickaxe-size", maxSuperPickaxeSize)
        );
        butcherDefaultRadius = Math.max(
                -1,
                source.getInt("limits.butcher-radius.default", "butcher-default-radius", butcherDefaultRadius)
        );
        butcherMaxRadius = Math.max(-1, source.getInt("limits.butcher-radius.maximum", "butcher-max-radius", butcherMaxRadius));
        // Inelegant but required to transfer to FAWE limits as defaults
        MAX_RADIUS = maxRadius;
        MAX_BRUSH_RADIUS = maxBrushRadius;
        MAX_SUPER_RADIUS = maxSuperPickaxeSize;
        MAX_BUTCHER_RADIUS = butcherMaxRadius;

        disallowedBlocks = source
                .getStringList("limits.disallowed-blocks", "disallowed-blocks", getDefaultDisallowedBlocks())
                .stream()
                .map(s -> s.contains(":") ? s.toLowerCase(Locale.ROOT) : ("minecraft:" + s).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        allowedDataCycleBlocks = new HashSet<>(source.getStringList("limits.allowed-data-cycle-blocks", null, List.of()));

        registerHelp = source.getBoolean("register-help", "register-help", registerHelp);
        logCommands = source.getBoolean("logging.log-commands", "log-commands", logCommands);
        logFile = source.getString("logging.file", "log-file", logFile);
        logFormat = source.getString("logging.format", "log-format", logFormat);

        superPickaxeDrop = source.getBoolean("super-pickaxe.drop-items", "super-pickaxe-drop-items", superPickaxeDrop);
        superPickaxeManyDrop = source.getBoolean(
                "super-pickaxe.many-drop-items",
                "super-pickaxe-many-drop-items",
                superPickaxeManyDrop
        );

        useInventory = source.getBoolean("use-inventory.enable", "use-inventory", useInventory);
        useInventoryOverride = source.getBoolean("use-inventory.allow-override", "use-inventory-override", useInventoryOverride);
        useInventoryCreativeOverride = source.getBoolean(
                "use-inventory.creative-mode-overrides",
                "use-inventory-creative-override",
                useInventoryCreativeOverride
        );

        navigationWand = convertLegacyItem(source.getString("navigation-wand.item", "nav-wand-item", navigationWand));
        navigationWandMaxDistance = source.getInt("navigation-wand.max-distance", "nav-wand-distance", navigationWandMaxDistance);
        navigationUseGlass = source.getBoolean("navigation.use-glass", "nav-use-glass", navigationUseGlass);

        scriptTimeout = source.getInt("scripting.timeout", "scripting-timeout", scriptTimeout);
        scriptsDir = source.getString("scripting.dir", "craftscript-dir", scriptsDir);

        calculationTimeout = source.getInt("calculation.timeout", "calculation-timeout", calculationTimeout);
        maxCalculationTimeout = source.getInt("calculation.max-timeout", "max-calculation-timeout", maxCalculationTimeout);

        saveDir = source.getString("saving.dir", "schematic-save-dir", saveDir);

        allowSymlinks = source.getBoolean("files.allow-symbolic-links", "allow-symbolic-links", allowSymlinks);
        showHelpInfo = source.getBoolean("show-help-on-first-use", null, showHelpInfo);
        serverSideCUI = source.getBoolean("server-side-cui", "server-side-cui", serverSideCUI);

        initializeSnapshotConfiguration(
                source.getString("snapshots.directory", "snapshots-dir", ""),
                source.getBoolean("snapshots.experimental", "snapshots-experimental", false)
        );

        String type = source.getString("shell-save-type", "shell-save-type", "").trim();
        shellSaveType = type.isEmpty() ? null : type;

        extendedYLimit = source.getBoolean("compat.extended-y-limit", "extended-y-limit", extendedYLimit);
        setDefaultLocaleName(source.getString("default-locale", "default-locale", defaultLocaleName));
        commandBlockSupport = source.getBoolean("command-block-support", "command-block-support", commandBlockSupport);
    }
    //FAWE end

    public boolean checkDisallowedBlocks(BlockStateHolder holder) {
        //FAWE start - rebuild the mask when the blocks were replaced, e.g. by a reload
        Set<String> blocks = disallowedBlocks;
        if (blocks.isEmpty()) {
            return false;
        }
        BlockMask mask = disallowedBlocksMask;
        if (mask == null || disallowedBlocksMaskSource != blocks) {
            BlockMaskBuilder builder = new BlockMaskBuilder();
            for (String blockRegex : blocks) {
                try {
                    builder.addRegex(blockRegex);
                } catch (InputParseException e) {
                    LOGGER.warn("Invalid disallowed block '{}'", blockRegex, e);
                }
            }
            mask = builder.build(new NullExtent());
            disallowedBlocksMask = mask;
            disallowedBlocksMaskSource = blocks;
        }
        return mask.test(holder.toImmutableState());
        //FAWE end
    }

    /**
     * Get the working directory to work from.
     *
     * @return a working directory
     * @deprecated Use {@link LocalConfiguration#getWorkingDirectoryPath()}
     */
    @Deprecated
    public File getWorkingDirectory() {
        return getWorkingDirectoryPath().toFile();
    }

    /**
     * Get the working directory to work from.
     *
     * @return a working directory
     */
    public Path getWorkingDirectoryPath() {
        return Paths.get(".");
    }

    public void initializeSnapshotConfiguration(String directory, boolean experimental) {
        // Reset for reload
        snapshotRepo = null;
        snapshotDatabase = null;
        snapshotsConfigured = false;
        if (!directory.isEmpty()) {
            if (experimental) {
                try {
                    snapshotDatabase = FileSystemSnapshotDatabase.maybeCreate(
                            Paths.get(directory),
                            ArchiveNioSupports.combined()
                    );
                    snapshotsConfigured = true;
                } catch (IOException e) {
                    LOGGER.warn("Failed to open snapshotDatabase", e);
                }
            } else {
                snapshotRepo = new SnapshotRepository(directory);
                snapshotsConfigured = true;
            }
        }
    }

    public String convertLegacyItem(String legacy) {
        String item = legacy;
        try {
            String[] splitter = item.split(":", 2);
            int id = 0;
            byte data = 0;
            if (splitter.length == 1) {
                id = Integer.parseInt(item);
            } else {
                id = Integer.parseInt(splitter[0]);
                data = Byte.parseByte(splitter[1]);
            }
            item = LegacyMapper.getInstance().getItemFromLegacy(id, data).id();
        } catch (Throwable ignored) {
        }

        return item;
    }

    public void setDefaultLocaleName(String localeName) {
        this.defaultLocaleName = localeName;
        if (localeName.equals("default")) {
            this.defaultLocale = Locale.getDefault();
        } else {
            this.defaultLocale = TextUtils.getLocaleByMinecraftTag(localeName);
        }
    }

}
