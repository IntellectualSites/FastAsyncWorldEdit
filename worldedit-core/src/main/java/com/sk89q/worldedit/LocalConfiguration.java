/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.sk89q.worldedit.util.logging.LogFormat;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import com.sk89q.worldedit.world.snapshot.SnapshotRepository;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents WorldEdit's configuration.
 */
public abstract class LocalConfiguration {
    protected static final String[] defaultDisallowedBlocks = new String[] {};

    public boolean profile = false;
    public Set<BlockType> disallowedBlocks = new HashSet<>();
    public int defaultChangeLimit = -1;
    public int maxChangeLimit = -1;
    public int defaultMaxPolygonalPoints = -1;
    public int maxPolygonalPoints = 20;
    public int defaultMaxPolyhedronPoints = -1;
    public int maxPolyhedronPoints = 20;
    public String shellSaveType = "";
    public SnapshotRepository snapshotRepo = null;
    public int maxRadius = -1;
    public int maxSuperPickaxeSize = 5;
    public int maxBrushRadius = 100;
    public boolean logCommands = false;
    public String logFile = "";
    public String logFormat = LogFormat.DEFAULT_FORMAT;
    public boolean registerHelp = true; // what is the point of this, it's not even used
    public String wandItem = ItemTypes.WOODEN_AXE.getId();
    public boolean superPickaxeDrop = true;
    public boolean superPickaxeManyDrop = true;
    public boolean noDoubleSlash = false;
    public boolean useInventory = false;
    public boolean useInventoryOverride = false;
    public boolean useInventoryCreativeOverride = false;
    public boolean navigationUseGlass = true;
    public String navigationWand = ItemTypes.COMPASS.getId();
    public int navigationWandMaxDistance = 50;
    public int scriptTimeout = 3000;
    public Set<BlockType> allowedDataCycleBlocks = new HashSet<>();
    public String saveDir = "schematics";
    public String scriptsDir = "craftscripts";
    public boolean showHelpInfo = true;
    public int butcherDefaultRadius = -1;
    public int butcherMaxRadius = -1;
    public boolean allowSymlinks = false;
    public boolean serverSideCUI = true;

    /**
     * Load the configuration.
     */
    public abstract void load();

    /**
     * Get the working directory to work from.
     *
     * @return a working directory
     */
    public File getWorkingDirectory() {
        return new File(".");
    }
}
