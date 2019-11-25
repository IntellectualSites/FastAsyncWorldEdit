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
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.implementation.processors.ChunkSendProcessor;
import com.boydti.fawe.beta.implementation.processors.NullProcessor;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.inject.InjectedValueAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.MethodCommands.getArguments;
import static com.sk89q.worldedit.command.util.Logging.LogMode.ALL;
import static com.sk89q.worldedit.command.util.Logging.LogMode.ORIENTATION_REGION;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

/**
 * Commands that operate on regions.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class RegionCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public RegionCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            name = "/air",
            aliases = {"/0"},
            desc = "Sets all the blocks in the region to air"
    )
    @CommandPermissions("worldedit.region.set")
    @Logging(REGION)
    public void air(Actor actor, EditSession editSession,
                    @Selection Region region,
                    InjectedValueAccess context) throws WorldEditException {
        set(actor, editSession, region, BlockTypes.AIR, context);
    }

    @Command(
            name = "/set",
            aliases = {"/"},
            desc = "Sets all the blocks in the region"
    )
    @CommandPermissions("worldedit.region.set")
    @Logging(REGION)
    public void set(Actor actor, EditSession editSession,
                    @Selection Region region,
                    @Arg(desc = "The pattern of blocks to set")
                            Pattern pattern, InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.setBlocks(region, pattern);
            if (affected != 0) {
                BBC.OPERATION.send(actor, affected);
                if (!actor.hasPermission("fawe.tips"))
                    BBC.TIP_FAST.or(BBC.TIP_CANCEL, BBC.TIP_MASK, BBC.TIP_MASK_ANGLE, BBC.TIP_SET_LINEAR, BBC.TIP_SURFACE_SPREAD, BBC.TIP_SET_HAND).send(actor);
            }
        }, "/set", region, context);
    }

    @Command(
            name = "/test",
            desc = "test region"
    )
    @CommandPermissions("worldedit.region.test")
    @Logging(REGION)
    public void test(Player player, EditSession editSession, @Selection Region region, @Arg(desc = "hello there") BiomeType biome) throws WorldEditException {
        System.out.println("Test start");
        editSession.addProcessor(new ChunkSendProcessor(editSession.getWorld(), () -> Collections.singleton(player)));
        editSession.addProcessor(NullProcessor.INSTANCE);
        FlatRegionFunction replace = new BiomeReplace(editSession, biome);
        FlatRegionVisitor visitor = new FlatRegionVisitor(Regions.asFlatRegion(region), replace);
        Operations.completeLegacy(visitor);
        System.out.println("Test end");
    }

    @Command(
            name = "/fixlighting",
            desc = "Get the light at a position"
    )
    @CommandPermissions("worldedit.light.fix")
    public void fixLighting(Player player) throws WorldEditException {
        final Location loc = player.getLocation();
        Region selection = player.getSelection();
        if (selection == null) {
            final int cx = loc.getBlockX() >> 4;
            final int cz = loc.getBlockZ() >> 4;
            selection = new CuboidRegion(BlockVector3.at(cx - 8, 0, cz - 8).multiply(16), BlockVector3.at(cx + 8, 0, cz + 8).multiply(16));
        }
        int count = FaweAPI.fixLighting(player.getWorld(), selection,null);
        BBC.LIGHTING_PROPAGATE_SELECTION.send(player, count);
    }

    @Command(
            name = "/getlighting",
            desc = "Get the light at a position"
    )
    @CommandPermissions("worldedit.light.fix")
    public void getLighting(Player player, EditSession editSession) throws WorldEditException {
        final Location loc = player.getLocation();
        int block = editSession.getBlockLight(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        int sky = editSession.getSkyLight(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        player.print("Light: " + block + " | " + sky);
    }

    @Command(
            name = "/removelighting",
            desc = "Removing lighting in a selection"
    )
    @CommandPermissions("worldedit.light.remove")
    public void removeLighting(Player player) {
        Region selection = player.getSelection();
        if (selection == null) {
            final int cx = player.getLocation().getBlockX() >> 4;
            final int cz = player.getLocation().getBlockZ() >> 4;
            selection = new CuboidRegion(BlockVector3.at(cx - 8, 0, cz - 8).multiply(16), BlockVector3.at(cx + 8, 0, cz + 8).multiply(16));
        }
        int count = FaweAPI.fixLighting(player.getWorld(), selection, null);
        BBC.UPDATED_LIGHTING_SELECTION.send(player, count);
    }

    @Command(
            name = "/nbtinfo",
            aliases = "/nbt",
            desc = "View nbt info for a block"
    )
    @CommandPermissions("worldedit.nbtinfo")
    public void nbtinfo(Player player, EditSession editSession) {
        Location pos = player.getBlockTrace(128);
        if (pos == null) {
            player.printError(BBC.NO_BLOCK.s());
            return;
        }
        CompoundTag nbt = editSession.getFullBlock(pos.toBlockPoint()).getNbtData();
        if (nbt != null) {
            player.print(nbt.getValue().toString());
        } else {
            player.printError(BBC.NO_BLOCK.s());
        }
    }

    @Command(
            name = "/setblocklight",
            desc = "Set block lighting in a selection"
    )
    @CommandPermissions("worldedit.light.set")
    public void setlighting(Player player, EditSession editSession, @Selection Region region, @Range(min = 0, max = 15) int value) {
        // TODO NOT IMPLEMENTED
    }

    @Command(
            name = "/setskylight",
            desc = "Set sky lighting in a selection"
    )
    @CommandPermissions("worldedit.light.set")
    public void setskylighting(Player player, @Selection Region region, @Range(min = 0, max = 15) int value) {
        // TODO NOT IMPLEMENTED
    }

    @Command(
        name = "/line",
        desc = "Draws a line segment between cuboid selection corners",
        descFooter = "Can only be used with a cuboid selection"
    )
    @CommandPermissions("worldedit.region.line")
    @Logging(REGION)
    public int line(Actor actor, EditSession editSession,
                    @Selection Region region,
                    @Arg(desc = "The pattern of blocks to place")
                        Pattern pattern,
                    @Range(min = 1) @Arg(desc = "The thickness of the line", def = "0")
                        int thickness,
                    @Switch(name = 'h', desc = "Generate only a shell")
                        boolean shell) throws WorldEditException {
        if (!(region instanceof CuboidRegion)) {
            actor.printError("//line only works with cuboid selections");
            return 0;
        }
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");

        CuboidRegion cuboidregion = (CuboidRegion) region;
        BlockVector3 pos1 = cuboidregion.getPos1();
        BlockVector3 pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(pattern, pos1, pos2, thickness, !shell);

        BBC.VISITOR_BLOCK.send(actor, blocksChanged);
        return blocksChanged;
    }

    @Command(
        name = "/curve",
        desc = "Draws a spline through selected points",
        descFooter = "Can only be used with a convex polyhedral selection"
    )
    @CommandPermissions("worldedit.region.curve")
    @Logging(REGION)
    public void curve(Actor actor, EditSession editSession,
                     @Selection Region region,
                     @Arg(desc = "The pattern of blocks to place")
                         Pattern pattern,
                     @Arg(desc = "The thickness of the curve", def = "0")
                         int thickness,
                     @Switch(name = 'h', desc = "Generate only a shell")
                         boolean shell, InjectedValueAccess context) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion)) {
            actor.printError("//curve only works with convex polyhedral selections");
            return;
        }
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");

        actor.checkConfirmationRegion(() -> {
        ConvexPolyhedralRegion cpregion = (ConvexPolyhedralRegion) region;
        List<BlockVector3> vectors = new ArrayList<>(cpregion.getVertices());

        int blocksChanged = editSession.drawSpline(pattern, vectors, 0, 0, 0, 10, thickness, !shell);

        BBC.VISITOR_BLOCK.send(actor, blocksChanged);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/replace",
        aliases = { "/repl", "/rep" },
        desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.region.replace")
    @Logging(REGION)
    public void replace(Actor actor, EditSession editSession, @Selection Region region,
                       @Arg(desc = "The mask representing blocks to replace", def = "")
                           Mask from,
                       @Arg(desc = "The pattern of blocks to replace with")
                           Pattern to, InjectedValueAccess context) throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        Mask finalFrom = from;
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.replaceBlocks(region, finalFrom, to);
            BBC.VISITOR_BLOCK.send(actor, affected);
            if (!actor.hasPermission("fawe.tips")) {
                BBC.TIP_REPLACE_ID
                    .or(BBC.TIP_REPLACE_LIGHT, BBC.TIP_REPLACE_MARKER, BBC.TIP_TAB_COMPLETE,
                        BBC.TIP_REPLACE_REGEX, BBC.TIP_REPLACE_REGEX_2, BBC.TIP_REPLACE_REGEX_3,
                        BBC.TIP_REPLACE_REGEX_4, BBC.TIP_REPLACE_REGEX_5).send(actor);
            }
        }, "/replace", region, context);
    }

    @Command(
        name = "/overlay",
        desc = "Set a block on top of blocks in the region"
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void overlay(Actor actor, EditSession editSession, @Selection Region region,
                       @Arg(desc = "The pattern of blocks to overlay")
                            Pattern pattern, InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.overlayCuboidBlocks(region, pattern);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
            name = "/lay",
            desc = "Set the top block in the region"
)
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void lay(Player player, EditSession editSession, @Selection Region region, @Arg(name = "pattern", desc = "The pattern of blocks to lay") Pattern patternArg, InjectedValueAccess context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            BlockVector3 max = region.getMaximumPoint();
            int maxY = max.getBlockY();
            Iterable<BlockVector2> flat = Regions.asFlatRegion(region).asFlatRegion();
            Iterator<BlockVector2> iter = flat.iterator();
            int y = 0;
            int affected = 0;
            while (iter.hasNext()) {
                BlockVector2 pos = iter.next();
                int x = pos.getBlockX();
                int z = pos.getBlockZ();
                y = editSession.getNearestSurfaceTerrainBlock(x, z, y, 0, maxY);
                editSession.setBlock(x, y, z, patternArg);
                affected++;
            }
            BBC.VISITOR_BLOCK.send(player, affected);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/center",
        aliases = { "/middle" },
        desc = "Set the center block(s)"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.region.center")
    public void center(Actor actor, EditSession editSession, @Selection Region region,
                      @Arg(desc = "The pattern of blocks to set")
                          Pattern pattern) throws WorldEditException {
        int affected = editSession.center(region, pattern);
        BBC.VISITOR_BLOCK.send(actor, affected);
    }

    @Command(
        name = "/naturalize",
        desc = "3 layers of dirt on top then rock below"
    )
    @CommandPermissions("worldedit.region.naturalize")
    @Logging(REGION)
    public void naturalize(Actor actor, EditSession editSession, @Selection Region region, InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.naturalizeCuboidBlocks(region);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/walls",
        desc = "Build the four sides of the selection"
    )
    @CommandPermissions("worldedit.region.walls")
    @Logging(REGION)
    public void walls(Actor actor, EditSession editSession, @Selection Region region,
                     @Arg(desc = "The pattern of blocks to set")
                         Pattern pattern, InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.makeWalls(region, pattern);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/faces",
        aliases = { "/outline" },
        desc = "Build the walls, ceiling, and floor of a selection"
    )
    @CommandPermissions("worldedit.region.faces")
    @Logging(REGION)
    public void faces(Actor actor, EditSession editSession, @Selection Region region,
                     @Arg(desc = "The pattern of blocks to set")
                         Pattern pattern, InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.makeCuboidFaces(region, pattern);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/smooth",
        desc = "Smooth the elevation in the selection",
        descFooter = "Example: '//smooth 1 grass_block,dirt,stone' would only smooth natural surface terrain."
    )
    @CommandPermissions("worldedit.region.smooth")
    @Logging(REGION)
    public void smooth(Actor actor, EditSession editSession, @Selection Region region,
                      @Arg(desc = "# of iterations to perform", def = "1")
                          int iterations,
                      @Arg(desc = "The mask of blocks to use as the height map", def = "")
                          Mask mask,
        @Switch(name = 's', desc = "TODO") boolean snow, InjectedValueAccess context) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long volume = (((long) max.getX() - (long) min.getX() + 1) * ((long) max.getY() - (long) min.getY() + 1) * ((long) max.getZ() - (long) min.getZ() + 1));
        FaweLimit limit = actor.getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw FaweCache.MAX_CHECKS;
        }
        actor.checkConfirmationRegion(() -> {
            try {
                HeightMap heightMap = new HeightMap(editSession, region, mask, snow);
                HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
                int affected = heightMap.applyFilter(filter, iterations);
                BBC.VISITOR_BLOCK.send(actor, affected);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, "/smooth", region, context);
    }

    @Command(
            name = "/wea",
            aliases = {"wea", "worldeditanywhere", "/worldeditanywhere", "/weanywhere"},
            desc = "Bypass region restrictions",
            descFooter = "Bypass region restrictions"
    )
    @CommandPermissions("fawe.admin")
    public void wea(Actor actor) throws WorldEditException {
        if (actor.togglePermission("fawe.bypass")) {
            actor.print(BBC.WORLDEDIT_BYPASSED.s());
        } else {
            actor.print(BBC.WORLDEDIT_RESTRICTED.s());
        }
    }

    @Command(
            name = "/wer",
            aliases = {"wer", "worldeditregion", "/worldeditregion", "select", "/select"},
            desc = "Select your current allowed region",
            descFooter = "Select your current allowed region"
    )
    @CommandPermissions("fawe.worldeditregion")
    public void wer(Player player) throws WorldEditException {
        final Region region = player.getLargestRegion();
        if (region == null) {
            player.print(BBC.NO_REGION.s());
        } else {
            player.setSelection(region);
            player.print(BBC.SET_REGION.s());
        }
    }


    @Command(
            name = "/move",
            aliases = {"/mv"},
            desc = "Move the contents of the selection"
    )
    @CommandPermissions("worldedit.region.move")
    @Logging(ORIENTATION_REGION)
    public void move(Actor actor, World world, EditSession editSession, LocalSession session,
                     @Selection Region region,
                    @Arg(desc = "# of blocks to move", def = "1")
                        int count,
                    @Arg(desc = "The direction to move", def = Direction.AIM)
                    @Direction(includeDiagonals = true)
                        BlockVector3 direction,
                    @Arg(desc = "The pattern of blocks to leave", def = "air")
                        Pattern replace,
                    @Switch(name = 's', desc = "Shift the selection to the target location")
                        boolean moveSelection,
                    @Switch(name = 'a', desc = "Ignore air blocks")
                        boolean ignoreAirBlocks,
                    @Switch(name = 'e', desc = "Ignore entities")
                        boolean skipEntities,
                    @Switch(name = 'b', desc = "Also copy biomes")
                        boolean copyBiomes,
                    InjectedValueAccess context) throws WorldEditException {
        checkCommandArgument(count >= 1, "Count must be >= 1");
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.moveRegion(region, direction, count, !ignoreAirBlocks, !skipEntities, copyBiomes, replace);

            if (moveSelection) {
                try {
                    region.shift(direction.multiply(count));

                    session.getRegionSelector(world).learnChanges();
                    session.getRegionSelector(world).explainRegionAdjust(actor, session);
                } catch (RegionOperationException e) {
                    actor.printError(e.getMessage());
                }
            }

            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
            name = "/fall",
            desc = "Have the blocks in the selection fall",
            descFooter = "Make the blocks in the selection fall\n" +
                   "The -m flag will only fall within the vertical selection."
)
    @CommandPermissions("worldedit.region.fall")
    @Logging(ORIENTATION_REGION)
    public void fall(Player player, EditSession editSession, LocalSession session,
                     @Selection Region region,
                     @Arg(desc = "BlockStateHolder", def = "air") BlockStateHolder replace,
                     @Switch(name = 'm', desc = "TODO") boolean notFullHeight,
                     InjectedValueAccess context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            int affected = editSession.fall(region, !notFullHeight, replace);
            BBC.VISITOR_BLOCK.send(player, affected);
        }, "/fall", region, context);
    }

    @Command(
        name = "/stack",
        desc = "Repeat the contents of the selection"
    )
    @CommandPermissions("worldedit.region.stack")
    @Logging(ORIENTATION_REGION)
    public void stack(Actor actor, World world, EditSession editSession, LocalSession session,
                     @Selection Region region,
                     @Arg(desc = "# of copies to stack", def = "1")
                         int count,
                     @Arg(desc = "The direction to stack", def = Direction.AIM)
                     @Direction(includeDiagonals = true)
                         BlockVector3 direction,
                     @Switch(name = 's', desc = "Shift the selection to the last stacked copy")
                         boolean moveSelection,
                     @Switch(name = 'a', desc = "Ignore air blocks")
                         boolean ignoreAirBlocks,
                     @Switch(name = 'e', desc = "Skip entities")
                         boolean skipEntities,
                     @Switch(name = 'b', desc = "Also copy biomes")
                         boolean copyBiomes,
                     @ArgFlag(name = 'm', desc = "Source mask", def="")
                         Mask sourceMask,
                    InjectedValueAccess context) throws WorldEditException {
        actor.checkConfirmationStack(() -> {
            if (sourceMask != null) {
                editSession.addSourceMask(sourceMask);
            }
            int affected = editSession.stackCuboidRegion(region, direction, count, !ignoreAirBlocks, !skipEntities, copyBiomes);

            if (moveSelection) {
                try {
                final BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint());

                final BlockVector3 shiftVector = direction.toVector3().multiply(count * (Math.abs(direction.dot(size)) + 1)).toBlockPoint();
                    region.shift(shiftVector);

                    session.getRegionSelector(world).learnChanges();
                    session.getRegionSelector(world).explainRegionAdjust(actor, session);
                } catch (RegionOperationException e) {
                    actor.printError(e.getMessage());
                }
            }

            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, count, context);
    }

    @Command(
        name = "/deform",
        desc = "Deforms a selected region with an expression",
        descFooter = "The expression is executed for each block and is expected\n" +
            "to modify the variables x, y and z to point to a new block\n" +
            "to fetch. See also https://tinyurl.com/weexpr"
    )
    @CommandPermissions("worldedit.region.deform")
    @Logging(ALL)
    public void deform(Actor actor, LocalSession session, EditSession editSession, InjectedValueAccess context,
                      @Selection Region region,
                      @Arg(desc = "The expression to use", variable = true)
                          List<String> expression,
                      @Switch(name = 'r', desc = "Use the game's coordinate origin")
                          boolean useRawCoords,
                      @Switch(name = 'o', desc = "Use the selection's center as origin")
                          boolean offset) throws WorldEditException {
        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(actor).toVector3();
            unit = Vector3.ONE;
        } else {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).divide(2);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit = unit.withX(1.0);
            if (unit.getY() == 0) unit = unit.withY(1.0);
            if (unit.getZ() == 0) unit = unit.withZ(1.0);
        }

        final Vector3 unit1 = unit;
        actor.checkConfirmationRegion(() -> {
            try {
                final int affected = editSession.deformRegion(region, zero, unit1, String.join(" ", expression), session.getTimeout());
                if (actor instanceof Player) {
                    ((Player) actor).findFreePosition();
                }
                BBC.VISITOR_BLOCK.send(actor, affected);
            } catch (ExpressionException e) {
                actor.printError(e.getMessage());
            }
        }, "/deform", region, context);
    }

    @Command(
            name = "/regen",
            desc = "Regenerates the contents of the selection",
            descFooter =
                    "Regenerates the contents of the current selection.\n" +
                            "This command might affect things outside the selection,\n" +
                            "if they are within the same chunk."
)
    @CommandPermissions("worldedit.regen")
    @Logging(REGION)
    public void regenerateChunk(Player player, LocalSession session, EditSession editSession, @Selection Region region,
                                @Arg(def = "", desc = "Regenerate with biome") BiomeType biome,
                                @Arg(def = "", desc = "Regenerate with seed") Long seed,
                                InjectedValueAccess context) throws WorldEditException {
        player.checkConfirmationRegion(() -> {
            Mask mask = session.getMask();
            session.setMask((Mask) null);
            session.setSourceMask((Mask) null);
            editSession.regenerate(region, biome, seed);
            session.setMask(mask);
            session.setSourceMask(mask);
            if (!player.hasPermission("fawe.tips")) {
                player.print(BBC.COMMAND_REGEN_2.s());
            } else if (biome == null) {
                BBC.COMMAND_REGEN_0.send(player);
                if (!player.hasPermission("fawe.tips")) player.print(BBC.TIP_REGEN_0.s());
            } else if (seed == null) {
                player.print(BBC.COMMAND_REGEN_1.s());
                if (!player.hasPermission("fawe.tips")) BBC.TIP_REGEN_1.send(player);
            } else {
                player.print(BBC.COMMAND_REGEN_2.s());
            }
        }, "/regen ", region, context);


    }

    @Command(
        name = "/hollow",
        desc = "Hollows out the object contained in this selection",
        descFooter = "Hollows out the object contained in this selection.\n" +
            "Optionally fills the hollowed out part with the given block.\n" +
            "Thickness is measured in manhattan distance."

    )
    @CommandPermissions("worldedit.region.hollow")
    @Logging(REGION)
    public void hollow(Actor actor, EditSession editSession,
                      @Selection Region region,
                      @Arg(desc = "Thickness of the shell to leave", def = "0")
                          int thickness,
                      @Arg(desc = "The pattern of blocks to replace the hollowed area with", def = "air")
                          Pattern pattern,
        @ArgFlag(name = 'm', desc = "Mask to hollow with") Mask mask,
                       InjectedValueAccess context) throws WorldEditException {
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");
        Mask finalMask = mask == null ? new SolidBlockMask(editSession) : mask;
        actor.checkConfirmationRegion(() -> {
            int affected = editSession.hollowOutRegion(region, thickness, pattern, finalMask);
            BBC.VISITOR_BLOCK.send(actor, affected);
        }, getArguments(context), region, context);
    }

    @Command(
        name = "/forest",
        desc = "Make a forest within the region"
    )
    @CommandPermissions("worldedit.region.forest")
    @Logging(REGION)
    public int forest(Actor actor, EditSession editSession, @Selection Region region,
                      @Arg(desc = "The type of tree to place", def = "tree")
                          TreeType type,
                      @Arg(desc = "The density of the forest", def = "5")
                          double density) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be in [0, 100]");
        int affected = editSession.makeForest(region, density / 100, type);
        BBC.COMMAND_TREE.send(actor, affected);
        return affected;
    }

    @Command(
        name = "/flora",
        desc = "Make flora within the region"
    )
    @CommandPermissions("worldedit.region.flora")
    @Logging(REGION)
    public void flora(Actor actor, EditSession editSession, @Selection Region region,
                     @Arg(desc = "The density of the forest", def = "5")
                         double density, InjectedValueAccess context) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be in [0, 100]");
        actor.checkConfirmationRegion(() -> {
            FloraGenerator generator = new FloraGenerator(editSession);
            GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
            LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
            visitor.setMask(new NoiseFilter2D(new RandomNoise(), density / 100));
            Operations.completeLegacy(visitor);

            BBC.COMMAND_FLORA.send(actor, ground.getAffected());
        }, "/flora", region, context);
    }

}
