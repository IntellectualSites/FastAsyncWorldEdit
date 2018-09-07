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

package com.sk89q.worldedit.command;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.visitor.Fast2DIterator;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.*;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ALL;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ORIENTATION_REGION;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

/**
 * Commands that operate on regions.
 */
@Command(aliases = {}, desc = "Commands that operate on regions: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Region_operations)")
public class RegionCommands extends MethodCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public RegionCommands(WorldEdit worldEdit) {
        super(worldEdit);
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = {"/fixlighting"},
            desc = "Get the light at a position",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.light.fix")
    public void fixlighting(Player player) throws WorldEditException {
        FawePlayer fp = FawePlayer.wrap(player);
        final FaweLocation loc = fp.getLocation();
        Region selection = fp.getSelection();
        if (selection == null) {
            final int cx = loc.x >> 4;
            final int cz = loc.z >> 4;
            selection = new CuboidRegion(new Vector(cx - 8, 0, cz - 8).multiply(16), new Vector(cx + 8, 0, cz + 8).multiply(16));
        }
        int count = FaweAPI.fixLighting(loc.world, selection, FaweQueue.RelightMode.ALL);
        BBC.LIGHTING_PROPOGATE_SELECTION.send(fp, count);
    }

    @Command(
            aliases = {"/getlighting"},
            desc = "Get the light at a position",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.light.fix")
    public void getlighting(Player player) throws WorldEditException {
        FawePlayer fp = FawePlayer.wrap(player);
        final FaweLocation loc = fp.getLocation();
        FaweQueue queue = fp.getFaweQueue(false);
        fp.sendMessage("Light: " + queue.getEmmittedLight(loc.x, loc.y, loc.z) + " | " + queue.getSkyLight(loc.x, loc.y, loc.z));
    }

    @Command(
            aliases = {"/removelight", "/removelighting"},
            desc = "Removing lighting in a selection",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.light.remove")
    public void removelighting(Player player) {
        FawePlayer fp = FawePlayer.wrap(player);
        final FaweLocation loc = fp.getLocation();
        Region selection = fp.getSelection();
        if (selection == null) {
            final int cx = loc.x >> 4;
            final int cz = loc.z >> 4;
            selection = new CuboidRegion(new Vector(cx - 8, 0, cz - 8).multiply(16), new Vector(cx + 8, 0, cz + 8).multiply(16));
        }
        int count = FaweAPI.fixLighting(loc.world, selection, FaweQueue.RelightMode.NONE);
        BBC.UPDATED_LIGHTING_SELECTION.send(fp, count);
    }

    @Command(
            aliases = {"/nbtinfo", "/nbt"},
            desc = "View nbt info for a block"
    )
    @CommandPermissions("worldedit.nbtinfo")
    public void nbtinfo(Player player, EditSession editSession) {
        Location pos = player.getBlockTrace(128);
        if (pos == null) {
            BBC.NO_BLOCK.send(player);
            return;
        }
        CompoundTag nbt = editSession.getBlock(pos.toVector()).getNbtData();
        if (nbt != null) {
            player.print(nbt.getValue().toString());
        } else {
            BBC.NO_BLOCK.send(player);
        }
    }

    @Command(
            aliases = {"/setblocklight", "/setlight"},
            desc = "Set block lighting in a selection",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.light.set")
    public void setlighting(Player player, @Selection Region region, @Range(min = 0, max = 15) int value) {
        FawePlayer fp = FawePlayer.wrap(player);
        final FaweLocation loc = fp.getLocation();
        final int cx = loc.x >> 4;
        final int cz = loc.z >> 4;
        final NMSMappedFaweQueue queue = (NMSMappedFaweQueue) fp.getFaweQueue(false);
        for (Vector pt : region) {
            queue.setBlockLight((int) pt.getX(), (int) pt.getY(), (int) pt.getZ(), value);
        }
        int count = 0;
        for (Vector2D chunk : region.getChunks()) {
            queue.sendChunk(queue.getFaweChunk(chunk.getBlockX(), chunk.getBlockZ()));
            count++;
        }
        BBC.UPDATED_LIGHTING_SELECTION.send(fp, count);
    }

    @Command(
            aliases = {"/setskylight"},
            desc = "Set sky lighting in a selection",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.light.set")
    public void setskylighting(Player player, @Selection Region region, @Range(min = 0, max = 15) int value) {
        FawePlayer fp = FawePlayer.wrap(player);
        final FaweLocation loc = fp.getLocation();
        final int cx = loc.x >> 4;
        final int cz = loc.z >> 4;
        final NMSMappedFaweQueue queue = (NMSMappedFaweQueue) fp.getFaweQueue(false);
        for (Vector pt : region) {
            queue.setSkyLight((int) pt.getX(), (int) pt.getY(), (int) pt.getZ(), value);
        }
        int count = 0;
        for (Vector2D chunk : region.getChunks()) {
            queue.sendChunk(queue.getFaweChunk(chunk.getBlockX(), chunk.getBlockZ()));
            count++;
        }
        BBC.UPDATED_LIGHTING_SELECTION.send(fp, count);
    }

    @Command(
            aliases = {"/line"},
            usage = "<pattern> [thickness]",
            desc = "Draws a line segment between cuboid selection corners",
            help =
                    "Draws a line segment between cuboid selection corners.\n" +
                            "Can only be used with cuboid selections.\n" +
                            "Flags:\n" +
                            "  -h generates only a shell",
            flags = "h",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.region.line")
    @Logging(REGION)
    public void line(Player player, EditSession editSession,
                     @Selection Region region,
                     Pattern pattern,
                     @Optional("0") @Range(min = 0) int thickness,
                     @Switch('h') boolean shell) throws WorldEditException {

        if (!(region instanceof CuboidRegion)) {
            player.printError("//line only works with cuboid selections");
            return;
        }

        CuboidRegion cuboidregion = (CuboidRegion) region;
        Vector pos1 = cuboidregion.getPos1();
        Vector pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(pattern, pos1, pos2, thickness, !shell);

        BBC.VISITOR_BLOCK.send(player, blocksChanged);
    }

    @Command(
            aliases = {"/curve", "/spline"},
            usage = "<pattern> [thickness]",
            desc = "Draws a spline through selected points",
            help =
                    "Draws a spline through selected points.\n" +
                            "Can only be used with convex polyhedral selections.\n" +
                            "Flags:\n" +
                            "  -h generates only a shell",
            flags = "h",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.region.curve")
    @Logging(REGION)
    public void curve(FawePlayer player, EditSession editSession,
                      @Selection Region region,
                      Pattern pattern,
                      @Optional("0") @Range(min = 0) int thickness,
                      @Switch('h') boolean shell,
                      CommandContext context) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion)) {
            player.sendMessage(BBC.getPrefix() + "//curve only works with convex polyhedral selections");
            return;
        }
        worldEdit.checkMaxRadius(thickness);

        player.checkConfirmationRegion(() -> {
            ConvexPolyhedralRegion cpregion = (ConvexPolyhedralRegion) region;
            List<Vector> vectors = new ArrayList<Vector>(cpregion.getVertices());

            int blocksChanged = editSession.drawSpline(pattern, vectors, 0, 0, 0, 10, thickness, !shell);

            BBC.VISITOR_BLOCK.send(player, blocksChanged);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/replace", "/re", "/rep", "/r"},
            usage = "[from-mask] <to-pattern>",
            desc = "Replace all blocks in the selection with another",
            flags = "f",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.region.replace")
    @Logging(REGION)
    public void replace(FawePlayer player, EditSession editSession, @Selection Region region, @Optional Mask from, Pattern to, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.replaceBlocks(region, from == null ? new ExistingBlockMask(editSession) : from, to);
            BBC.VISITOR_BLOCK.send(player, affected);
            if (!player.hasPermission("fawe.tips"))
                BBC.TIP_REPLACE_ID.or(BBC.TIP_REPLACE_LIGHT, BBC.TIP_REPLACE_MARKER, BBC.TIP_TAB_COMPLETE, BBC.TIP_REPLACE_REGEX, BBC.TIP_REPLACE_REGEX_2, BBC.TIP_REPLACE_REGEX_3, BBC.TIP_REPLACE_REGEX_4, BBC.TIP_REPLACE_REGEX_5).send(player);
        }, getArguments(context), region);
    }

    // Compatibility for SKCompat
    @Deprecated
    public void set(Player player, LocalSession session, EditSession editSession, Pattern pattern) throws WorldEditException {
        set(FawePlayer.wrap(player), session, editSession, session.getSelection(player.getWorld()), pattern, null);
    }

    @Command(
            aliases = {"/set", "/s"},
            usage = "[pattern]",
            desc = "Set all blocks within selection",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.region.set")
    @Logging(REGION)
    public void set(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region selection, Pattern to, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected;
            affected = editSession.setBlocks(selection, to);
            if (affected != 0) {
                BBC.OPERATION.send(player, affected);
                if (!player.hasPermission("fawe.tips"))
                    BBC.TIP_FAST.or(BBC.TIP_CANCEL, BBC.TIP_MASK, BBC.TIP_MASK_ANGLE, BBC.TIP_SET_LINEAR, BBC.TIP_SURFACE_SPREAD, BBC.TIP_SET_HAND).send(player);
            }
        }, getArguments(context), selection);
    }

    @Command(
            aliases = {"/overlay"},
            usage = "<pattern>",
            desc = "Set a block on top of blocks in the region",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void overlay(FawePlayer player, EditSession editSession, @Selection Region region, Pattern pattern, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.overlayCuboidBlocks(region, pattern);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/lay"},
            usage = "<pattern>",
            desc = "Set the top block in the region",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void lay(FawePlayer player, EditSession editSession, @Selection Region region, Pattern pattern, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();
            int maxY = max.getBlockY();
            int width = region.getWidth();
            int height = region.getLength();
            int bx = min.getBlockX();
            int bz = min.getBlockZ();
            Iterable<Vector2D> flat = Regions.asFlatRegion(region).asFlatRegion();
            Iterator<Vector2D> iter = new Fast2DIterator(flat, editSession).iterator();
            int y = 0;
            int affected = 0;
            MutableBlockVector mutable = new MutableBlockVector();
            while (iter.hasNext()) {
                Vector2D pos = iter.next();
                int x = pos.getBlockX();
                int z = pos.getBlockZ();
                y = editSession.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
                editSession.setBlock(x, y, z, pattern);
                affected++;
            }
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/center", "/middle"},
            usage = "<pattern>",
            desc = "Set the center block(s)",
            min = 1,
            max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.region.center")
    public void center(Player player, EditSession editSession, @Selection Region region, Pattern pattern) throws WorldEditException {
        int affected = editSession.center(region, pattern);
        BBC.VISITOR_BLOCK.send(player, affected);
    }

    @Command(
            aliases = {"/naturalize"},
            usage = "",
            desc = "3 layers of dirt on top then rock below",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.region.naturalize")
    @Logging(REGION)
    public void naturalize(FawePlayer player, EditSession editSession, @Selection Region region, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.naturalizeCuboidBlocks(region);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/walls"},
            usage = "<pattern>",
            desc = "Build the four sides of the selection",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.region.walls")
    @Logging(REGION)
    public void walls(FawePlayer player, EditSession editSession, @Selection Region region, Pattern pattern, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.makeWalls(region, pattern);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/faces", "/outline"},
            usage = "<pattern>",
            desc = "Build the walls, ceiling, and floor of a selection",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.region.faces")
    @Logging(REGION)
    public void faces(FawePlayer player, EditSession editSession, @Selection Region region, Pattern pattern, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.makeCuboidFaces(region, pattern);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/smooth"},
            usage = "[iterations]",
            flags = "n",
            desc = "Smooth the elevation in the selection",
            help =
                    "Smooths the elevation in the selection.\n" +
                            "The -n flag makes it only consider naturally occuring blocks.\n" +
                            "The -s flag makes it only consider snow.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.smoothsnow")
    @Logging(REGION)
    public void smooth(FawePlayer player, EditSession editSession, @Selection Region region, @Optional("1") int iterations, @Switch('n') boolean affectNatural, @Switch('s') boolean snow, CommandContext context) throws WorldEditException {
        try {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();
            long volume = (((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1));
            FaweLimit limit = FawePlayer.wrap(player).getLimit();
            if (volume >= limit.MAX_CHECKS) {
                throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
            }
            player.checkConfirmationRegion(() -> {
                HeightMap heightMap = new HeightMap(editSession, region, affectNatural, snow);
                HeightMapFilter filter = (HeightMapFilter) HeightMapFilter.class.getConstructors()[0].newInstance(GaussianKernel.class.getConstructors()[0].newInstance(5, 1));
                int affected = heightMap.applyFilter(filter, iterations);
                BBC.VISITOR_BLOCK.send(player, affected);
            }, getArguments(context), region);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Command(
            aliases = {"/wea", "wea", "worldeditanywhere", "/worldeditanywhere", "/weanywhere"},
            desc = "Bypass region restrictions",
            help = "Bypass region restrictions"
    )
    @CommandPermissions("fawe.admin")
    public void wea(Player player) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (fp.toggle("fawe.bypass")) {
            BBC.WORLDEDIT_BYPASSED.send(fp);
        } else {
            BBC.WORLDEDIT_RESTRICTED.send(fp);
        }
    }

    @Command(
            aliases = {"/wer", "wer", "worldeditregion", "/worldeditregion", "select", "/select"},
            desc = "Select your current allowed region",
            help = "Select your current allowed region"
    )
    @CommandPermissions("fawe.worldeditregion")
    public void wer(Player player) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        final Region region = fp.getLargestRegion();
        if (region == null) {
            BBC.NO_REGION.send(fp);
        } else {
            fp.setSelection(region);
            BBC.SET_REGION.send(fp);
        }
    }


    @Command(
            aliases = {"/move"},
            usage = "[count] [direction] [leave-id]",
            flags = "s",
            desc = "Move the contents of the selection",
            help =
                    "Moves the contents of the selection.\n" +
                            "  -s flag shifts the selection to the target location.\n" +
                            "  -b also copies biomes\n" +
                            "  -e ignores entities\n" +
                            "  -a ignores air\n" +
                            "Optionally fills the old location with <leave-id>.",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.region.move")
    @Logging(ORIENTATION_REGION)
    public void move(FawePlayer player, LocalSession session, EditSession editSession,
                     @Selection Region region,
                     @Optional("1") @Range(min = 1) int count,
                     @Optional(Direction.AIM) @Direction Vector direction,
                     @Optional("air") Pattern replace,
                     @Switch('b') boolean copyBiomes,
                     @Switch('e') boolean skipEntities,
                     @Switch('a') boolean skipAir,
                     @Switch('s') boolean moveSelection,
                     CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.moveRegion(region, direction, count, !skipAir, !skipEntities, copyBiomes, replace);

            if (moveSelection) {
                try {
                    region.shift(direction.multiply(count));

                    session.getRegionSelector(player.getWorld()).learnChanges();
                    session.getRegionSelector(player.getWorld()).explainRegionAdjust(player.getPlayer(), session);
                } catch (RegionOperationException e) {
                    player.sendMessage(BBC.getPrefix() + e.getMessage());
                }
            }

            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/fall"},
            usage = "[replace]",
            flags = "m",
            desc = "Have the blocks in the selection fall",
            help =
                    "Make the blocks in the selection fall\n" +
                            "The -m flag will only fall within the vertical selection.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.fall")
    @Logging(ORIENTATION_REGION)
    public void fall(FawePlayer player, EditSession editSession, LocalSession session,
                     @Selection Region region,
                     @Optional("air") BlockStateHolder replace,
                     @Switch('m') boolean notFullHeight,
                     CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.fall(region, !notFullHeight, replace);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/stack"},
            usage = "[count] [direction]",
            flags = "sam",
            desc = "Repeat the contents of the selection",
            help =
                    "Repeats the contents of the selection.\n" +
                            "Flags:\n" +
                            "  -s shifts the selection to the last stacked copy\n" +
                            "  -a skips air blocks",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.stack")
    @Logging(ORIENTATION_REGION)
    public void stack(FawePlayer player, LocalSession session, EditSession editSession,
                      @Selection Region region,
                      @Optional("1") @Range(min = 1) int count,
                      @Optional(Direction.AIM) @Direction Vector direction,
                      @Switch('s') boolean moveSelection,
                      @Switch('b') boolean copyBiomes,
                      @Switch('e') boolean skipEntities,
                      @Switch('a') boolean ignoreAirBlocks, @Switch('m') Mask sourceMask, CommandContext context) throws WorldEditException {
        player.checkConfirmationStack(() -> {
            if (sourceMask != null) {
                editSession.addSourceMask(sourceMask);
            }
            int affected = editSession.stackCuboidRegion(region, direction, count, !ignoreAirBlocks, !skipEntities, copyBiomes);

            if (moveSelection) {
                try {
                    final Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
                    Vector shiftVector = new Vector(direction.getX() * size.getX() * count, direction.getY() * size.getY() * count, direction.getZ() * size.getZ() * count);
                    region.shift(shiftVector);

                    session.getRegionSelector(player.getWorld()).learnChanges();
                    session.getRegionSelector(player.getWorld()).explainRegionAdjust(player.getPlayer(), session);
                } catch (RegionOperationException e) {
                    player.sendMessage(BBC.getPrefix() + e.getMessage());
                }
            }

            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region, count);
    }

    @Command(
            aliases = {"/deform"},
            usage = "<expression>",
            desc = "Deforms a selected region with an expression",
            help =
                    "Deforms a selected region with an expression\n" +
                            "The expression is executed for each block and is expected\n" +
                            "to modify the variables x, y and z to point to a new block\n" +
                            "to fetch. See also tinyurl.com/wesyntax.",
            flags = "ro",
            min = 1,
            max = -1
    )
    @CommandPermissions("worldedit.region.deform")
    @Logging(ALL)
    public void deform(FawePlayer fp, Player player, LocalSession session, EditSession editSession,
                       @Selection Region region,
                       @Text String expression,
                       @Switch('r') boolean useRawCoords,
                       @Switch('o') boolean offset,
                       CommandContext context) throws WorldEditException {
        fp.checkConfirmationRegion(() -> {
            final Vector zero;
            Vector unit;

            if (useRawCoords) {
                zero = Vector.ZERO;
                unit = Vector.ONE;
            } else if (offset) {
                zero = session.getPlacementPosition(player);
                unit = Vector.ONE;
            } else {
                final Vector min = region.getMinimumPoint();
                final Vector max = region.getMaximumPoint();

                zero = max.add(min).multiply(0.5);
                unit = max.subtract(zero);

                if (unit.getX() == 0) unit.mutX(1);
                if (unit.getY() == 0) unit.mutY(1);
                if (unit.getZ() == 0) unit.mutZ(1);
            }

            try {
                final int affected = editSession.deformRegion(region, zero, unit, expression);
                player.findFreePosition();
                BBC.VISITOR_BLOCK.send(fp, affected);
            } catch (ExpressionException e) {
                fp.sendMessage(BBC.getPrefix() + e.getMessage());
            }
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/regen"},
            usage = "[biome] [seed]",
            desc = "Regenerates the contents of the selection",
            help =
                    "Regenerates the contents of the current selection.\n" +
                            "This command might affect things outside the selection,\n" +
                            "if they are within the same chunk.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.regen")
    @Logging(REGION)
    public void regenerateChunk(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region region, CommandContext args) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            Mask mask = session.getMask();
            Mask sourceMask = session.getSourceMask();
            session.setMask((Mask) null);
            session.setSourceMask((Mask) null);
            BaseBiome biome = null;
            if (args.argsLength() >= 1) {
                BiomeRegistry biomeRegistry = worldEdit.getPlatformManager().queryCapability(Capability.GAME_HOOKS).getRegistries().getBiomeRegistry();
                List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
                biome = Biomes.findBiomeByName(knownBiomes, args.getString(0), biomeRegistry);
            }
            Long seed = args.argsLength() != 2 || !MathMan.isInteger(args.getString(1)) ? null : Long.parseLong(args.getString(1));
            editSession.regenerate(region, biome, seed);
            session.setMask(mask);
            session.setSourceMask(mask);
            if (!player.hasPermission("fawe.tips")) {
                BBC.COMMAND_REGEN_2.send(player);
            } else if (biome == null) {
                BBC.COMMAND_REGEN_0.send(player);
                if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_REGEN_0.send(player);
            } else if (seed == null) {
                BBC.COMMAND_REGEN_1.send(player);
                if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_REGEN_1.send(player);
            } else {
                BBC.COMMAND_REGEN_2.send(player);
            }
        }, getArguments(args), region);


    }

    @Command(
            aliases = {"/hollow"},
            usage = "[<thickness>[ <pattern>]]",
            desc = "Hollows out the object contained in this selection",
            help =
                    "Hollows out the object contained in this selection.\n" +
                            "Optionally fills the hollowed out part with the given block.\n" +
                            "Thickness is measured in manhattan distance.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.hollow")
    @Logging(REGION)
    public void hollow(FawePlayer player, EditSession editSession,
                       @Selection Region region,
                       @Optional("0") @Range(min = 0) int thickness,
                       @Optional("air") Pattern pattern,
                       CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.hollowOutRegion(region, thickness, pattern);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region);
    }

    @Command(
            aliases = {"/forest"},
            usage = "[type] [density]",
            desc = "Make a forest within the region",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.region.forest")
    @Logging(REGION)
    public void forest(FawePlayer player, EditSession editSession, @Selection Region region, @Optional("tree") TreeType type,
                       @Optional("5") @Range(min = 0, max = 100) double density,
                       CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            ForestGenerator generator = new ForestGenerator(editSession, type);
            GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
            LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
            visitor.setMask(new NoiseFilter2D(new RandomNoise(), density / 100));
            Operations.completeLegacy(visitor);

            BBC.COMMAND_TREE.send(player, ground.getAffected());
        }, getArguments(context), region);

    }

    @Command(
            aliases = {"/flora"},
            usage = "[density]",
            desc = "Make flora within the region",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.region.flora")
    @Logging(REGION)
    public void flora(FawePlayer player, EditSession editSession, @Selection Region region, @Optional("10") @Range(min = 0, max = 100) double density, CommandContext context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            FloraGenerator generator = new FloraGenerator(editSession);
            GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
            LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
            visitor.setMask(new NoiseFilter2D(new RandomNoise(), density / 100));
            Operations.completeLegacy(visitor);

            BBC.COMMAND_FLORA.send(player, ground.getAffected());
        }, getArguments(context), region);
    }


}