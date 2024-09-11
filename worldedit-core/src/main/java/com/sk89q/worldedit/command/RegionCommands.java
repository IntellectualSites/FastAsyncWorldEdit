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

package com.sk89q.worldedit.command;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.command.util.annotation.Preload;
import com.sk89q.worldedit.command.util.annotation.SynchronousSettingExpected;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Offset;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.math.convolution.SnowHeightMap;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Create a new instance.
     */
    public RegionCommands() {
    }

    @Command(
            name = "/set",
            desc = "Sets all the blocks in the region"
    )
    @CommandPermissions("worldedit.region.set")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    public int set(
            Actor actor, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern
    ) {
        int affected = editSession.setBlocks(region, pattern);
        if (affected != 0) {
            actor.print(Caption.of("worldedit.set.done", TextComponent.of(affected)));

        }
        return affected;
    }

    @Command(
            name = "/air",
            aliases = {"/0"},
            desc = "Sets all the blocks in the region to air"
    )
    @CommandPermissions("worldedit.region.set")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    public void air(Actor actor, EditSession editSession, @Selection Region region) throws WorldEditException {
        set(actor, editSession, region, BlockTypes.AIR);
    }

    @Command(
            name = "/test",
            desc = "test region"
    )
    @CommandPermissions(
            value = "worldedit.region.test",
            queued = false
    )
    @Logging(REGION)
    public void test(
            Actor actor, EditSession editSession,
            @Arg(desc = "test") double testValue
    ) throws WorldEditException {
        actor.print(TextComponent.of(testValue));
    }

    @Command(
            name = "/fixlighting",
            desc = "Get the light at a position"
    )
    @CommandPermissions("worldedit.light.fix")
    public void fixLighting(Actor actor, LocalSession session, @Selection Region selection) throws WorldEditException {
        int count = FaweAPI.fixLighting(session.getSelectionWorld(), selection, null, RelightMode.ALL);
        actor.print(Caption.of("fawe.info.lighting.propagate.selection", count));
    }

    @Command(
            name = "/removelighting",
            aliases = "/removelight",
            desc = "Removing lighting in a selection"
    )
    @CommandPermissions("worldedit.light.remove")
    public void removeLighting(Actor actor, LocalSession session, @Selection Region selection) {
        int count = FaweAPI.fixLighting(session.getSelectionWorld(), selection, null, RelightMode.NONE);
        actor.print(Caption.of("fawe.info.updated.lighting.selection", count));
    }

    @Command(
            name = "/nbtinfo",
            aliases = "/nbt",
            desc = "View nbt info for a block"
    )
    @CommandPermissions(
            value = "worldedit.nbtinfo",
            queued = false
    )
    public void nbtinfo(Player player, EditSession editSession) {
        Location pos = player.getBlockTrace(128);
        if (pos == null) {
            player.print(Caption.of("fawe.navigation.no.block"));
            return;
        }
        CompoundTag nbt = editSession.getFullBlock(pos.toBlockPoint()).getNbtData();
        if (nbt != null) {
            player.print(TextComponent.of(nbt.getValue().toString()));
        } else {
            player.print(Caption.of("fawe.navigation.no.block"));
        }
    }

    @Deprecated(since = "2.4.2", forRemoval = true)
    @Command(
            name = "/setblocklight",
            aliases = "/setlight",
            desc = "Set block lighting in a selection"
    )
    @CommandPermissions("worldedit.light.set")
    public void setlighting(Actor actor, EditSession editSession, @Selection Region region) {
        actor.print(Caption.of("fawe.info.light-blocks"));
    }

    @Deprecated(since = "2.4.2", forRemoval = true)
    @Command(
            name = "/setskylight",
            desc = "Set sky lighting in a selection"
    )
    @CommandPermissions("worldedit.light.set")
    public void setskylighting(Actor actor, @Selection Region region) {
        actor.print(Caption.of("fawe.info.light-blocks"));
    }

    @Command(
            name = "/line",
            desc = "Draws line segments between cuboid selection corners or convex polyhedral selection vertices",
            descFooter = "Can only be used with a cuboid selection or a convex polyhedral selection"
    )
    @CommandPermissions("worldedit.region.line")
    @Logging(REGION)
    @SynchronousSettingExpected
    public int line(
            Actor actor, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "The pattern of blocks to place")
                    Pattern pattern,
            @Arg(desc = "The thickness of the line", def = "0")
                    double thickness,
            @Switch(name = 'h', desc = "Generate only a shell")
                    boolean shell
    ) throws WorldEditException {
        if (!(region instanceof CuboidRegion cuboidregion)) {
            actor.print(Caption.of("worldedit.line.cuboid-only"));
            return 0;
        }
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");

        BlockVector3 pos1 = cuboidregion.getPos1();
        BlockVector3 pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(pattern, pos1, pos2, thickness, !shell);

        actor.print(Caption.of("worldedit.line.changed", TextComponent.of(blocksChanged)));
        return blocksChanged;
    }

    @Command(
            name = "/curve",
            desc = "Draws a spline through selected points",
            descFooter = "Can only be used with a convex polyhedral selection"
    )
    @CommandPermissions("worldedit.region.curve")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    @SynchronousSettingExpected
    public int curve(
            Actor actor, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "The pattern of blocks to place")
                    Pattern pattern,
            @Arg(desc = "The thickness of the curve", def = "0")
                    double thickness,
            @Switch(name = 'h', desc = "Generate only a shell")
                    boolean shell
    ) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion cpregion)) {
            actor.print(Caption.of("worldedit.curve.invalid-type"));
            return 0;
        }
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");

        List<BlockVector3> vectors = new ArrayList<>(cpregion.getVertices());

        int blocksChanged = editSession.drawSpline(pattern, vectors, 0, 0, 0, 10, thickness, !shell);

        actor.print(Caption.of("worldedit.curve.changed", TextComponent.of(blocksChanged)));
        return blocksChanged;
    }

    @Command(
            name = "/replace",
            aliases = {"/re", "/rep"},
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.region.replace")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @Confirm(Confirm.Processor.REGION)
    public int replace(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The mask representing blocks to replace", def = "")
                    Mask from,
            @Arg(desc = "The pattern of blocks to replace with")
                    Pattern to
    ) throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
            //FAWE start > the mask will have been initialised with a WorldWrapper extent (very bad/slow
        } else {
            new MaskTraverser(from).setNewExtent(editSession);
        }
        int affected = editSession.replaceBlocks(region, from, to);
        actor.print(Caption.of("worldedit.replace.replaced", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/overlay",
            desc = "Set a block on top of blocks in the region"
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    @SynchronousSettingExpected // TODO improve using filter/chunk-based-placement
    public int overlay(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The pattern of blocks to overlay")
                    Pattern pattern
    ) throws WorldEditException {
        int affected = editSession.overlayCuboidBlocks(region, pattern);
        actor.print(Caption.of("worldedit.overlay.overlaid", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/lay",
            desc = "Set the top block in the region"
    )
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @Confirm(Confirm.Processor.REGION)
    @SynchronousSettingExpected // TODO improve using filter/chunk-based-placement
    public void lay(
            Actor actor,
            EditSession editSession,
            @Selection Region region,
            @Arg(name = "pattern", desc = "The pattern of blocks to lay") Pattern patternArg
    ) throws WorldEditException {
        //FAWE start - world min/maxY
        int maxY = region.getMaximumY();
        int minY = region.getMinimumY();
        //FAWE end
        Iterable<BlockVector2> flat = Regions.asFlatRegion(region).asFlatRegion();
        Iterator<BlockVector2> iter = flat.iterator();
        //FAWE start - world min/maxY
        int y = minY;
        //FAWE end
        int affected = 0;
        while (iter.hasNext()) {
            BlockVector2 pos = iter.next();
            int x = pos.x();
            int z = pos.z();
            //FAWE start - world min/maxY
            y = editSession.getNearestSurfaceTerrainBlock(x, z, y, minY, maxY);
            //FAWE end
            editSession.setBlock(x, y, z, patternArg);
            affected++;
        }
        actor.print(Caption.of("fawe.worldedit.visitor.visitor.block", affected));
    }

    @Command(
            name = "/center",
            aliases = {"/middle"},
            desc = "Set the center block(s)"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.region.center")
    @SynchronousSettingExpected
    public int center(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern
    ) throws WorldEditException {
        int affected = editSession.center(region, pattern);
        actor.print(Caption.of("worldedit.center.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/naturalize",
            desc = "3 layers of dirt on top then rock below"
    )
    @CommandPermissions("worldedit.region.naturalize")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    @SynchronousSettingExpected // TODO improve using filter/chunk-based-placement
    @Preload(Preload.PreloadCheck.PRELOAD)
    public int naturalize(Actor actor, EditSession editSession, @Selection Region region) throws WorldEditException {
        int affected = editSession.naturalizeCuboidBlocks(region);
        actor.print(Caption.of("worldedit.naturalize.naturalized", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/walls",
            desc = "Build the four sides of the selection"
    )
    @CommandPermissions("worldedit.region.walls")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    public int walls(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern
    ) throws WorldEditException {
        int affected = editSession.makeWalls(region, pattern);
        actor.print(Caption.of("worldedit.walls.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/faces",
            aliases = {"/outline"},
            desc = "Build the walls, ceiling, and floor of a selection"
    )
    @CommandPermissions("worldedit.region.faces")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @Confirm(Confirm.Processor.REGION)
    public int faces(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The pattern of blocks to set")
                    Pattern pattern
    ) throws WorldEditException {
        int affected = editSession.makeFaces(region, pattern);
        actor.print(Caption.of("worldedit.faces.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/smooth",
            desc = "Smooth the elevation in the selection",
            descFooter = "Example: '//smooth 1 grass_block,dirt,stone' would only smooth natural surface terrain."
    )
    @CommandPermissions("worldedit.region.smooth")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @Confirm(Confirm.Processor.REGION)
    @SynchronousSettingExpected
    public int smooth(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "# of iterations to perform", def = "1")
                    int iterations,
            @Arg(desc = "The mask of blocks to use as the height map", def = "")
                    Mask mask
    ) throws WorldEditException {
        //FAWE start > the mask will have been initialised with a WorldWrapper extent (very bad/slow)
        new MaskTraverser(mask).setNewExtent(editSession);
        //FAWE end
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long volume = (((long) max.x() - (long) min.x() + 1) * ((long) max.y() - (long) min.y() + 1) * ((long) max.z() - (long) min
                .z() + 1));
        FaweLimit limit = actor.getLimit();
        if (volume >= limit.MAX_CHECKS.get()) {
            throw FaweCache.MAX_CHECKS;
        }
        int affected;
        try {
            HeightMap heightMap = new HeightMap(editSession, region, mask);
            HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
            affected = heightMap.applyFilter(filter, iterations);
            actor.print(Caption.of("worldedit.smooth.changed", TextComponent.of(affected)));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return affected;
    }

    @Command(
            name = "/wea",
            aliases = {"wea", "worldeditanywhere", "/worldeditanywhere", "/weanywhere"},
            desc = "Bypass region restrictions",
            descFooter = "Bypass region restrictions"
    )
    @CommandPermissions(
            value = "fawe.admin",
            queued = false
    )
    public void wea(Actor actor) throws WorldEditException {
        if (actor.togglePermission("fawe.bypass")) {
            actor.print(Caption.of("fawe.info.worldedit.bypassed"));
        } else {
            actor.print(Caption.of("fawe.info.worldedit.restricted"));
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
            player.print(Caption.of("fawe.error.no.region"));
        } else {
            player.setSelection(region);
            player.print(Caption.of("fawe.info.set.region"));
        }
    }

    @Command(
            name = "/snowsmooth",
            desc = "Smooth the elevation in the selection with snow layers",
            descFooter = "Example: '//snowsmooth 1 -m snow_block,snow' would only smooth snow terrain."
    )
    @CommandPermissions("worldedit.region.snowsmooth")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int snowSmooth(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "# of iterations to perform", def = "1")
                    int iterations,
            @ArgFlag(name = 'l', desc = "Set the amount of snow blocks under the snow", def = "1")
                    int snowBlockCount,
            @ArgFlag(name = 'm', desc = "The mask of blocks to use as the height map")
                    Mask mask
    ) throws WorldEditException {
        SnowHeightMap heightMap = new SnowHeightMap(editSession, region, mask);
        HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
        float[] changed = heightMap.applyFilter(filter, iterations);
        int affected = heightMap.applyChanges(changed, snowBlockCount);
        actor.print(Caption.of("worldedit.snowsmooth.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/move",
            aliases = {"/mv"},
            desc = "Move the contents of the selection"
    )
    @CommandPermissions("worldedit.region.move")
    @Logging(ORIENTATION_REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int move(
            Actor actor, World world, EditSession editSession, LocalSession session,
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
            @Switch(name = 'e', desc = "Also copy entities")
                    boolean copyEntities,
            @Switch(name = 'b', desc = "Also copy biomes")
                    boolean copyBiomes,
            @ArgFlag(name = 'm', desc = "Set the include mask, non-matching blocks become air")
                    Mask mask
    ) throws WorldEditException {
        checkCommandArgument(count >= 1, "Multiplier must be >= 1");

        //FAWE start > the mask will have been initialised with a WorldWrapper extent (very bad/slow)
        new MaskTraverser(mask).setNewExtent(editSession);
        //FAWE end
        Mask combinedMask;
        if (ignoreAirBlocks) {
            if (mask == null) {
                combinedMask = new ExistingBlockMask(editSession);
            } else {
                combinedMask = new MaskIntersection(mask, new ExistingBlockMask(editSession));
            }
        } else {
            combinedMask = mask;
        }

        int affected = editSession.moveRegion(region, direction, count, copyEntities, copyBiomes, combinedMask, replace);

        if (moveSelection) {
            try {
                region.shift(direction.multiply(count));

                session.getRegionSelector(world).learnChanges();
                session.getRegionSelector(world).explainRegionAdjust(actor, session);
            } catch (RegionOperationException e) {
                actor.printError(e.getRichMessage());
            }
        }

        actor.print(Caption.of("worldedit.move.moved", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/fall",
            desc = "Have the blocks in the selection fall",
            descFooter = "Make the blocks in the selection fall\n"
    )
    @CommandPermissions("worldedit.region.fall")
    @Logging(ORIENTATION_REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public void fall(
            Actor actor, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "BlockStateHolder", def = "air")
                    BlockStateHolder replace,
            @Switch(name = 'm', desc = "Only fall within the vertical selection")
                    boolean notFullHeight
    ) throws WorldEditException {
        int affected = editSession.fall(region, !notFullHeight, replace);
        actor.print(Caption.of("fawe.worldedit.visitor.visitor.block", affected));
    }

    @Command(
            name = "/stack",
            desc = "Repeat the contents of the selection"
    )
    @CommandPermissions("worldedit.region.stack")
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Logging(ORIENTATION_REGION)
    public int stack(
            Actor actor, World world, EditSession editSession, LocalSession session,
            @Selection Region region,
            @Arg(desc = "# of copies to stack", def = "1")
            @Confirm(Confirm.Processor.REGION)
                    int count,
            @Arg(desc = "How far to move the contents each stack", def = Offset.FORWARD)
            @Offset
                    BlockVector3 offset,
            @Switch(name = 's', desc = "Shift the selection to the last stacked copy")
                    boolean moveSelection,
            @Switch(name = 'a', desc = "Ignore air blocks")
                    boolean ignoreAirBlocks,
            @Switch(name = 'e', desc = "Also copy entities")
                    boolean copyEntities,
            @Switch(name = 'b', desc = "Also copy biomes")
                    boolean copyBiomes,
            @Switch(name = 'r', desc = "Use block units")
                    boolean blockUnits,
            @ArgFlag(name = 'm', desc = "Set the include mask, non-matching blocks become air")
                    Mask mask
    ) throws WorldEditException {
        checkCommandArgument(count >= 1, "Count must be >= 1");

        //FAWE start > the mask will have been initialised with a WorldWrapper extent (very bad/slow)
        new MaskTraverser(mask).setNewExtent(editSession);
        //FAWE end
        Mask combinedMask;
        if (ignoreAirBlocks) {
            if (mask == null) {
                combinedMask = new ExistingBlockMask(editSession);
            } else {
                combinedMask = new MaskIntersection(mask, new ExistingBlockMask(editSession));
            }
        } else {
            combinedMask = mask;
        }

        int affected;
        if (blockUnits) {
            affected = editSession.stackRegionBlockUnits(region, offset, count, copyEntities, copyBiomes, combinedMask);
        } else {
            affected = editSession.stackCuboidRegion(region, offset, count, copyEntities, copyBiomes, combinedMask);
        }

        if (moveSelection) {
            try {
                final BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
                final BlockVector3 shiftSize = blockUnits ? offset : offset.multiply(size);
                final BlockVector3 shiftVector = shiftSize.multiply(count);
                region.shift(shiftVector);

                session.getRegionSelector(world).learnChanges();
                session.getRegionSelector(world).explainRegionAdjust(actor, session);
            } catch (RegionOperationException e) {
                actor.printError(e.getRichMessage());
            }
        }

        actor.print(Caption.of("worldedit.stack.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/regen",
            desc = "Regenerates the contents of the selection",
            descFooter = "This command might affect things outside the selection,\n"
                    + "if they are within the same chunk."
    )
    @CommandPermissions("worldedit.regen")
    @Logging(REGION)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    void regenerate(
            Actor actor, World world, LocalSession session, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "The seed to regenerate with, otherwise uses world seed", def = "")
                    Long seed,
            @Switch(name = 'b', desc = "Regenerate biomes as well")
                    boolean regenBiomes,
            @Switch(name = 'r', desc = "If the seed should be randomized")
                    boolean randomSeed,
            @Arg(desc = "Biome to apply for this regeneration (only works in overworld)", def = "")
                    BiomeType biomeType
    ) throws WorldEditException {
        Mask mask = session.getMask();
        boolean success;
        try {
            session.setMask(null);
            //FAWE start
            session.setSourceMask(null);
            actor.print(Caption.of("fawe.regen.time"));
            //FAWE end
            RegenOptions options = RegenOptions.builder()
                    .seed(!randomSeed ? seed : Long.valueOf(ThreadLocalRandom.current().nextLong()))
                    .regenBiomes(regenBiomes)
                    .biomeType(biomeType)
                    .build();
            success = world.regenerate(region, editSession, options);
        } finally {
            session.setMask(mask);
            //FAWE start
            session.setSourceMask(mask);
            //FAWE end
        }
        if (actor instanceof Player && Settings.settings().GENERAL.UNSTUCK_ON_GENERATE) {
            ((Player) actor).findFreePosition();
        }
        if (success) {
            actor.print(Caption.of("worldedit.regen.regenerated"));
        } else {
            actor.print(Caption.of("worldedit.regen.failed"));
        }
    }

    @Command(
            name = "/deform",
            desc = "Deforms a selected region with an expression",
            descFooter = """
                    The expression is executed for each block and is expected
                    to modify the variables x, y and z to point to a new block
                    to fetch. For details, see https://ehub.to/we/expr"""
    )
    @CommandPermissions("worldedit.region.deform")
    @Logging(ALL)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int deform(
            Actor actor, LocalSession session, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "The expression to use", variable = true)
                    List<String> expression,
            @Switch(name = 'r', desc = "Use the game's coordinate origin")
                    boolean useRawCoords,
            @Switch(name = 'o', desc = "Use the placement's coordinate origin")
                    boolean offset,
            @Switch(name = 'c', desc = "Use the selection's center as origin")
                    boolean offsetCenter
    ) throws WorldEditException {
        final Vector3 zero;
        Vector3 unit;

        if (useRawCoords) {
            zero = Vector3.ZERO;
            unit = Vector3.ONE;
        } else if (offsetCenter) {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).multiply(0.5);
            unit = Vector3.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(actor).toVector3();
            unit = Vector3.ONE;
        } else {
            final Vector3 min = region.getMinimumPoint().toVector3();
            final Vector3 max = region.getMaximumPoint().toVector3();

            zero = max.add(min).divide(2);
            unit = max.subtract(zero);

            if (unit.x() == 0) {
                unit = unit.withX(1.0);
            }
            if (unit.y() == 0) {
                unit = unit.withY(1.0);
            }
            if (unit.z() == 0) {
                unit = unit.withZ(1.0);
            }
        }

        final Vector3 unit1 = unit;
        try {
            final int affected = editSession.deformRegion(
                    region,
                    zero,
                    unit1,
                    String.join(" ", expression),
                    session.getTimeout()
            );
            if (actor instanceof Player && Settings.settings().GENERAL.UNSTUCK_ON_GENERATE) {
                ((Player) actor).findFreePosition();
            }
            actor.print(Caption.of("worldedit.deform.deformed", TextComponent.of(affected)));
            return affected;
        } catch (ExpressionException e) {
            actor.printError(TextComponent.of(e.getMessage()));
            return 0;
        }
    }

    @Command(
            name = "/hollow",
            desc = "Hollows out the object contained in this selection",
            descFooter = """
                    Hollows out the object contained in this selection.
                    Optionally fills the hollowed out part with the given block.
                    Thickness is measured in manhattan distance."""
    )
    @CommandPermissions("worldedit.region.hollow")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int hollow(
            Actor actor, EditSession editSession,
            @Selection Region region,
            @Arg(desc = "Thickness of the shell to leave", def = "0")
                    int thickness,
            @Arg(desc = "The pattern of blocks to replace the hollowed area with", def = "air")
                    Pattern pattern,
            @ArgFlag(name = 'm', desc = "Mask to hollow with")
                    Mask mask
    ) throws WorldEditException {
        checkCommandArgument(thickness >= 0, "Thickness must be >= 0");
        //FAWE start > the mask will have been initialised with a WorldWrapper extent (very bad/slow)
        Mask finalMask;
        if (mask != null) {
            new MaskTraverser(mask).setNewExtent(editSession);
            finalMask = mask;
        } else {
            finalMask = new SolidBlockMask(editSession);
        }
        //FAWE end

        int affected = editSession.hollowOutRegion(region, thickness, pattern, finalMask);
        actor.print(Caption.of("worldedit.hollow.changed", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/forest",
            desc = "Make a forest within the region"
    )
    @CommandPermissions("worldedit.region.forest")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int forest(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The type of tree to place", def = "tree")
                    TreeType type,
            @Arg(desc = "The density of the forest", def = "5")
                    double density
    ) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be in [0, 100]");
        int affected = editSession.makeForest(region, density / 100, type);
        actor.print(Caption.of("worldedit.forest.created", TextComponent.of(affected)));
        return affected;
    }

    @Command(
            name = "/flora",
            desc = "Make flora within the region"
    )
    @CommandPermissions("worldedit.region.flora")
    @Logging(REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    @SynchronousSettingExpected
    @Confirm(Confirm.Processor.REGION)
    public int flora(
            Actor actor, EditSession editSession, @Selection Region region,
            @Arg(desc = "The density of the forest", def = "5")
                    double density
    ) throws WorldEditException {
        checkCommandArgument(0 <= density && density <= 100, "Density must be in [0, 100]");
        density = density / 100;
        FloraGenerator generator = new FloraGenerator(editSession);
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
        //FAWE start - provide extent for preloading
        LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
        //FAWE end
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);

        int affected = ground.getAffected();
        actor.print(Caption.of("worldedit.flora.created", TextComponent.of(affected)));
        return affected;
    }

    //FAWE start - block "fixing"
    @Command(
            name = "/fixblocks",
            aliases = {"/updateblocks", "/fixconnect"},
            desc = "\"Fixes\" all blocks in the region to the correct shape/connections based on surrounding blocks"
    )
    @CommandPermissions("worldedit.region.fixblocks")
    @Logging(REGION)
    @Confirm(Confirm.Processor.REGION)
    @Preload(Preload.PreloadCheck.PRELOAD)
    public int fixblocks(
            Actor actor, EditSession editSession, @Selection Region region
    ) {
        int affected = editSession.setBlocks(
                region,
                WorldEdit
                        .getInstance()
                        .getPlatformManager()
                        .queryCapability(Capability.WORLD_EDITING)
                        .getPlatformPlacementProcessor(editSession, null, region)
        );
        if (affected != 0) {
            actor.print(Caption.of("worldedit.set.done", TextComponent.of(affected)));

        }
        return affected;
    }
    //FAWE end

}
