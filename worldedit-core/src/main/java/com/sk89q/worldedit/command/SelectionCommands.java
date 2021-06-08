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

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.regions.selector.FuzzyRegionSelector;
import com.boydti.fawe.object.regions.selector.PolyhedralRegionSelector;
import com.google.common.base.Strings;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.command.argument.SelectorChoice;
import com.sk89q.worldedit.command.tool.NavigationWand;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Locatable;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.MultiDirection;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.util.formatting.component.InvalidComponentException;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.component.SubtleFormat;
import com.sk89q.worldedit.util.formatting.component.TextComponentProducer;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.item.ItemType;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.storage.ChunkStore;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;
import org.enginehub.piston.exception.StopExecutionException;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.sk89q.worldedit.command.util.Logging.LogMode.POSITION;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;

/**
 * Selection commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class SelectionCommands {

    private final WorldEdit we;

    public SelectionCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        name = "/pos1",
        aliases = "/1",
        desc = "Set position 1"
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos1(Actor actor, World world, LocalSession session,
                     @Arg(desc = "Coordinates to set position 1 to", def = "")
                         BlockVector3 coordinates) throws WorldEditException {
        Location pos;
        if (coordinates != null) {
            pos = new Location(world, coordinates.toVector3().clampY(0, world.getMaxY()));
        } else if (actor instanceof Locatable) {
            pos = ((Locatable) actor).getBlockLocation().clampY(0, world.getMaxY());
        } else {
            actor.print(Caption.of("worldedit.pos.console-require-coords"));
            return;
        }

        if (!session.getRegionSelector(world).selectPrimary(pos.toVector().toBlockPoint(), ActorSelectorLimits.forActor(actor))) {
            actor.print(Caption.of("worldedit.pos.already-set"));
            return;
        }

        session.getRegionSelector(world)
                .explainPrimarySelection(actor, session, pos.toVector().toBlockPoint());
    }

    @Command(
        name = "/pos2",
            aliases = "/2",
        desc = "Set position 2"
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos2(Actor actor, World world, LocalSession session,
                     @Arg(desc = "Coordinates to set position 2 to", def = "")
                         BlockVector3 coordinates) throws WorldEditException {
        Location pos;
        if (coordinates != null) {
            pos = new Location(world, coordinates.toVector3().clampY(0, world.getMaxY()));
        } else if (actor instanceof Locatable) {
            pos = ((Locatable) actor).getBlockLocation().clampY(0, world.getMaxY());
        } else {
            actor.print(Caption.of("worldedit.pos.console-require-coords"));
            return;
        }

        if (!session.getRegionSelector(world).selectSecondary(pos.toVector().toBlockPoint(), ActorSelectorLimits.forActor(actor))) {
            actor.print(Caption.of("worldedit.pos.already-set"));
            return;
        }

        session.getRegionSelector(world)
                .explainSecondarySelection(actor, session, pos.toVector().toBlockPoint());
    }

    @Command(
        name = "/hpos1",
        desc = "Set position 1 to targeted block"
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos1(Player player, LocalSession session) throws WorldEditException {

        Location pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectPrimary(pos.toVector().toBlockPoint(), ActorSelectorLimits.forActor(player))) {
                player.print(Caption.of("worldedit.hpos.already-set"));
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainPrimarySelection(player, session, pos.toBlockPoint());
        } else {
            player.print(Caption.of("worldedit.hpos.no-block"));
        }
    }

    @Command(
        name = "/hpos2",
        desc = "Set position 2 to targeted block"
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos2(Player player, LocalSession session) throws WorldEditException {

        Location pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectSecondary(pos.toVector().toBlockPoint(), ActorSelectorLimits.forActor(player))) {
                player.print(Caption.of("worldedit.hpos.already-set"));
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainSecondarySelection(player, session, pos.toBlockPoint());
        } else {
            player.print(Caption.of("worldedit.hpos.no-block"));
        }
    }

    @Command(
        name = "/chunk",
        desc = "Set the selection to your current chunk.",
        descFooter = "This command selects 256-block-tall areas,\nwhich can be specified by the y-coordinate.\nE.g. -c x,1,z will select from y=256 to y=511."
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.chunk")
    public void chunk(Actor actor, World world, LocalSession session,
                      @Arg(desc = "The chunk to select", def = "")
                          BlockVector2 coordinates,
                      @Switch(name = 's', desc = "Expand your selection to encompass all chunks that are part of it")
                          boolean expandSelection,
                      @Switch(name = 'c', desc = "Use chunk coordinates instead of block coordinates")
                          boolean useChunkCoordinates) throws WorldEditException {
        final BlockVector3 min;
        final BlockVector3 max;
        if (expandSelection) {
            Region region = session.getSelection(world);

            final BlockVector2 min2D = ChunkStore.toChunk(region.getMinimumPoint());
            final BlockVector2 max2D = ChunkStore.toChunk(region.getMaximumPoint());

            min = BlockVector3.at(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = BlockVector3.at(max2D.getBlockX() * 16 + 15, world.getMaxY(), max2D.getBlockZ() * 16 + 15);

            actor.print(Caption.of(
                    "worldedit.chunk.selected-multiple",
                    TextComponent.of(min2D.getBlockX()),
                    TextComponent.of(min2D.getBlockZ()),
                    TextComponent.of(max2D.getBlockX()),
                    TextComponent.of(max2D.getBlockZ())
            ));
        } else {
            final BlockVector2 min2D;
            if (coordinates != null) {
                // coords specified
                min2D = useChunkCoordinates
                    ? coordinates
                    : ChunkStore.toChunk(coordinates.toBlockVector3());
            } else {
                // use player loc
                if (actor instanceof Locatable) {
                    min2D = ChunkStore.toChunk(((Locatable) actor).getBlockLocation().toVector().toBlockPoint());
                } else {
                    throw new StopExecutionException(TextComponent.of("A player or coordinates are required."));
                }
            }

            min = BlockVector3.at(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = min.add(15, world.getMaxY(), 15);

            actor.print(Caption.of("worldedit.chunk.selected", TextComponent.of(min2D.getBlockX()),
                    TextComponent.of(min2D.getBlockZ())));
        }

        final CuboidRegionSelector selector;
        if (session.getRegionSelector(world) instanceof ExtendingCuboidRegionSelector) {
            selector = new ExtendingCuboidRegionSelector(world);
        } else {
            selector = new CuboidRegionSelector(world);
        }
        selector.selectPrimary(min, ActorSelectorLimits.forActor(actor));
        selector.selectSecondary(max, ActorSelectorLimits.forActor(actor));
        session.setRegionSelector(world, selector);

        session.dispatchCUISelection(actor);

    }

    @Command(
        name = "/wand",
        desc = "Get the wand object"
    )
    @CommandPermissions("worldedit.wand")
    public void wand(Player player, LocalSession session,
                        @Switch(name = 'n', desc = "Get a navigation wand") boolean navWand) throws WorldEditException {
        session.loadDefaults(player, true);
        String wandId = navWand ? session.getNavWandItem() : session.getWandItem();
        if (wandId == null) {
            wandId = navWand ? we.getConfiguration().navigationWand : we.getConfiguration().wandItem;
        }
        ItemType itemType = ItemTypes.parse(wandId);
        if (itemType == null) {
            player.print(Caption.of("worldedit.wand.invalid"));
            return;
        }
        player.giveItem(new BaseItemStack(itemType, 1));
        if (navWand) {
            session.setTool(itemType, NavigationWand.INSTANCE);
            player.print(Caption.of("worldedit.wand.navwand.info"));
        } else {
            session.setTool(itemType, SelectionWand.INSTANCE);
            player.print(Caption.of("worldedit.wand.selwand.info"));
        }
    }

    @Command(
        name = "toggleeditwand",
        aliases = { "/toggleeditwand" },
        desc = "Remind the user that the wand is now a tool and can be unbound with /tool none."
    )
    @CommandPermissions("worldedit.wand.toggle")
    public void toggleWand(Player player) {
        player.print(
                Caption.of(
                        "worldedit.wand.selwand.now.tool",
                        TextComponent.of("/tool none").clickEvent(
                                ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/tool none")),
                        TextComponent.of("/tool selwand").clickEvent(
                                ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/tool selwand")),
                        TextComponent.of("//wand").clickEvent(
                                ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "//wand"))
                )
        );
    }

    @Command(
        name = "/contract",
        desc = "Contract the selection area"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.contract")
    public void contract(Actor actor, World world, LocalSession session,
                         @Arg(desc = "Amount to contract the selection by")
                             int amount,
                         @Arg(desc = "Amount to contract the selection by in the other direction", def = "0")
                             int reverseAmount,
                         @Arg(desc = "Direction to contract", def = Direction.AIM)
                         @MultiDirection
                             List<BlockVector3> direction) throws WorldEditException {
        try {
            Region region = session.getSelection(world);
            long oldSize = region.getVolume();
            if (reverseAmount == 0) {
                for (BlockVector3 dir : direction) {
                    region.contract(dir.multiply(amount));
                }
            } else {
                for (BlockVector3 dir : direction) {
                    region.contract(dir.multiply(amount), dir.multiply(-reverseAmount));
                }
            }
            session.getRegionSelector(world).learnChanges();
            long newSize = region.getVolume();

            session.getRegionSelector(world).explainRegionAdjust(actor, session);

            actor.print(Caption.of("worldedit.contract.contracted", TextComponent.of(oldSize - newSize)));
        } catch (RegionOperationException e) {
            actor.printError(TextComponent.of(e.getMessage()));
        }
    }

    @Command(
        name = "/shift",
        desc = "Shift the selection area"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.shift")
    public void shift(Actor actor, World world, LocalSession session,
                      @Arg(desc = "Amount to shift the selection by")
                          int amount,
                      @Arg(desc = "Direction to contract", def = Direction.AIM)
                      @MultiDirection
                          List<BlockVector3> direction) throws WorldEditException {
        try {
            Region region = session.getSelection(world);

            for (BlockVector3 dir : direction) {
                region.shift(dir.multiply(amount));
            }

            session.getRegionSelector(world).learnChanges();

            session.getRegionSelector(world).explainRegionAdjust(actor, session);

            actor.print(Caption.of("worldedit.shift.shifted"));
        } catch (RegionOperationException e) {
            actor.printError(TextComponent.of(e.getMessage()));
        }
    }

    @Command(
        name = "/outset",
        desc = "Outset the selection area"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.outset")
    public void outset(Actor actor, World world, LocalSession session,
                       @Arg(desc = "Amount to expand the selection by in all directions")
                           int amount,
                       @Switch(name = 'h', desc = "Only expand horizontally")
                           boolean onlyHorizontal,
                       @Switch(name = 'v', desc = "Only expand vertically")
                           boolean onlyVertical) throws WorldEditException {
        Region region = session.getSelection(world);
        region.expand(getChangesForEachDir(amount, onlyHorizontal, onlyVertical));
        session.getRegionSelector(world).learnChanges();
        session.getRegionSelector(world).explainRegionAdjust(actor, session);
        actor.print(Caption.of("worldedit.outset.outset"));
    }

    @Command(
        name = "/inset",
        desc = "Inset the selection area"
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.inset")
    public void inset(Actor actor, World world, LocalSession session,
                      @Arg(desc = "Amount to contract the selection by in all directions")
                          int amount,
                      @Switch(name = 'h', desc = "Only contract horizontally")
                          boolean onlyHorizontal,
                      @Switch(name = 'v', desc = "Only contract vertically")
                          boolean onlyVertical) throws WorldEditException {
        Region region = session.getSelection(world);
        region.contract(getChangesForEachDir(amount, onlyHorizontal, onlyVertical));
        session.getRegionSelector(world).learnChanges();
        session.getRegionSelector(world).explainRegionAdjust(actor, session);
        actor.print(Caption.of("worldedit.inset.inset"));
    }

    private BlockVector3[] getChangesForEachDir(int amount, boolean onlyHorizontal, boolean onlyVertical) {
        Stream.Builder<BlockVector3> changes = Stream.builder();

        if (!onlyHorizontal) {
            changes.add(BlockVector3.UNIT_Y);
            changes.add(BlockVector3.UNIT_MINUS_Y);
        }

        if (!onlyVertical) {
            changes.add(BlockVector3.UNIT_X);
            changes.add(BlockVector3.UNIT_MINUS_X);
            changes.add(BlockVector3.UNIT_Z);
            changes.add(BlockVector3.UNIT_MINUS_Z);
        }

        return changes.build().map(v -> v.multiply(amount)).toArray(BlockVector3[]::new);
    }

    @Command(
        name = "/size",
        desc = "Get information about the selection"
    )
    @CommandPermissions("worldedit.selection.size")
    public void size(Actor actor, World world, LocalSession session,
                     @Switch(name = 'c', desc = "Get clipboard info instead")
                         boolean clipboardInfo) throws WorldEditException {
        Region region;
        if (clipboardInfo) {
            ClipboardHolder root = session.getClipboard();
            int index = 0;
            for (ClipboardHolder holder : root.getHolders()) {
                Clipboard clipboard = holder.getClipboard();
                String name;
                if (holder instanceof URIClipboardHolder) {
                    URI uri = ((URIClipboardHolder) holder).getUri();
                    if (uri.toString().startsWith("file:/")) {
                        name = new File(uri.getPath()).getName();
                    } else {
                        name = uri.getFragment();
                    }
                } else {
                    name = Integer.toString(index);
                }

                region = clipboard.getRegion();
                BlockVector3 size = region.getMaximumPoint()
                        .subtract(region.getMinimumPoint()).add(1, 1, 1);
                BlockVector3 origin = clipboard.getOrigin();

                String sizeStr = size.getBlockX() + "*" + size.getBlockY() + "*" + size.getBlockZ();
                String originStr = origin.getBlockX() + "," + origin.getBlockY() + "," + origin.getBlockZ();

                long numBlocks = ((long) size.getBlockX() * size.getBlockY() * size.getBlockZ());
                actor.print(Caption.of("worldedit.size.offset", TextComponent.of(name), TextComponent.of(sizeStr), TextComponent.of(originStr), TextComponent.of(numBlocks)));
                index++;
            }
            return;
        } else {
            region = session.getSelection(world);

            actor.print(Caption.of("worldedit.size.type", TextComponent.of(session.getRegionSelector(world).getTypeName())));

            for (Component line : session.getRegionSelector(world).getSelectionInfoLines()) {
                actor.printInfo(line);
            }
        }
        BlockVector3 size = region.getMaximumPoint()
                .subtract(region.getMinimumPoint())
                .add(1, 1, 1);

        actor.print(Caption.of("worldedit.size.size", TextComponent.of(size.toString())));
        actor.print(Caption.of("worldedit.size.distance", TextComponent.of(region.getMaximumPoint().distance(region.getMinimumPoint()))));
        actor.print(Caption.of("worldedit.size.blocks", TextComponent.of(region.getVolume())));
    }

    @Command(
        name = "/count",
        desc = "Counts the number of blocks matching a mask"
    )
    @CommandPermissions("worldedit.analysis.count")
    public int count(Actor actor, World world, LocalSession session, EditSession editSession,
                      @Arg(desc = "The mask of blocks to match")
                          Mask mask) throws WorldEditException {
        int count = editSession.countBlocks(session.getSelection(world), mask);
        actor.print(Caption.of("worldedit.count.counted", TextComponent.of(count)));
        return count;
    }

    @Command(
        name = "/distr",
        desc = "Get the distribution of blocks in the selection"
    )
    @CommandPermissions("worldedit.analysis.distr")
    public void distr(Actor actor, World world, LocalSession session,
                      @Switch(name = 'c', desc = "Get the distribution of the clipboard instead")
                          boolean clipboardDistr,
                      @Switch(name = 'd', desc = "Separate blocks by state")
                          boolean separateStates,
                      @ArgFlag(name = 'p', desc = "Gets page from a previous distribution.")
                          Integer page) throws WorldEditException {
        List<Countable<BlockState>> distribution;

        if (page == null) {
            if (clipboardDistr) {
                Clipboard clipboard = session.getClipboard().getClipboard(); // throws if missing
                BlockDistributionCounter count = new BlockDistributionCounter(clipboard, separateStates);
                RegionVisitor visitor = new RegionVisitor(clipboard.getRegion(), count);
                Operations.completeBlindly(visitor);
                distribution = count.getDistribution();
            } else {
                try (EditSession editSession = session.createEditSession(actor)) {
                    distribution = editSession.getBlockDistribution(session.getSelection(world), separateStates);
                }
            }
            session.setLastDistribution(distribution);
            page = 1;
        } else {
            distribution = session.getLastDistribution();
            if (distribution == null) {
                actor.print(Caption.of("worldedit.distr.no-previous"));
                return;
            }
        }

        if (distribution.isEmpty()) {  // *Should* always be false
            actor.print(Caption.of("worldedit.distr.no-blocks"));
            return;
        }

        BlockDistributionResult res = new BlockDistributionResult(distribution, separateStates);
        if (!actor.isPlayer()) {
            res.formatForConsole();
        }
        actor.print(res.create(page));
    }

    @Command(
        name = "/sel",
        aliases = { ";", "/desel", "/deselect" },
        desc = "Choose a region selector"
    )
    @CommandPermissions("worldedit.analysis.sel")
    public void select(Actor actor, World world, LocalSession session,
                       @Arg(desc = "Selector to switch to", def = "")
                           SelectorChoice selector,
                       @Switch(name = 'd', desc = "Set default selector")
                           boolean setDefaultSelector) throws WorldEditException {
        if (selector == null) {
            session.getRegionSelector(world).clear();
            session.dispatchCUISelection(actor);
            actor.print(Caption.of("worldedit.select.cleared"));
            return;
        }

        final RegionSelector oldSelector = session.getRegionSelector(world);

        final RegionSelector newSelector;
        switch (selector) {
            case CUBOID:
                newSelector = new CuboidRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.cuboid.message"));
                break;
            case EXTEND:
                newSelector = new ExtendingCuboidRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.extend.message"));
                break;
            case POLY: {
                newSelector = new Polygonal2DRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.poly.message"));
                Optional<Integer> limit = ActorSelectorLimits.forActor(actor).getPolygonVertexLimit();
                limit.ifPresent(integer -> actor.print(Caption.of("worldedit.select.poly.limit-message", TextComponent.of(integer))));
                break;
            }
            case ELLIPSOID:
                newSelector = new EllipsoidRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.ellipsoid.message"));
                break;
            case SPHERE:
                newSelector = new SphereRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.sphere.message"));
                break;
            case CYL:
                newSelector = new CylinderRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.cyl.message"));
                break;
            case CONVEX:
            case HULL:
            case POLYHEDRON: {
                newSelector = new ConvexPolyhedralRegionSelector(oldSelector);
                actor.print(Caption.of("worldedit.select.convex.message"));
                Optional<Integer> limit = ActorSelectorLimits.forActor(actor).getPolyhedronVertexLimit();
                limit.ifPresent(integer -> actor.print(Caption.of("worldedit.select.convex.limit-message", TextComponent.of(integer))));
                break;
            }
            case POLYHEDRAL:
                newSelector = new PolyhedralRegionSelector(world);
                actor.print(Caption.of("fawe.selection.sel.convex.polyhedral"));
                Optional<Integer> limit = ActorSelectorLimits.forActor(actor).getPolyhedronVertexLimit();
                limit.ifPresent(integer -> actor.print(Caption.of("fawe.selection.sel.max", integer)));
                actor.print(Caption.of("fawe.selection.sel.list"));
                break;
            case FUZZY:
            case MAGIC:
                Mask maskOpt = new IdMask(world);
                //TODO Make FuzzyRegionSelector accept actors
                newSelector = new FuzzyRegionSelector((Player) actor, world, maskOpt);
                actor.print(Caption.of("fawe.selection.sel.fuzzy"));
                actor.print(Caption.of("fawe.selection.sel.list"));
                break;
            case LIST:
            default:
                CommandListBox box = new CommandListBox("Selection modes", null, null);
                box.setHidingHelp(true);
                TextComponentProducer contents = box.getContents();
                contents.append(SubtleFormat.wrap("Select one of the modes below:")).newline();

                box.appendCommand("cuboid", Caption.of("worldedit.select.cuboid.description"), "//sel cuboid");
                box.appendCommand("extend", Caption.of("worldedit.select.extend.description"), "//sel extend");
                box.appendCommand("poly", Caption.of("worldedit.select.poly.description"), "//sel poly");
                box.appendCommand("ellipsoid", Caption.of("worldedit.select.ellipsoid.description"), "//sel ellipsoid");
                box.appendCommand("sphere", Caption.of("worldedit.select.sphere.description"), "//sel sphere");
                box.appendCommand("cyl", Caption.of("worldedit.select.cyl.description"), "//sel cyl");
                box.appendCommand("convex", Caption.of("worldedit.select.convex.description"), "//sel convex");
                box.appendCommand("polyhedral", Caption.of("fawe.selection.sel.polyhedral"), "//sel polyhedral");
                box.appendCommand("fuzzy[=<mask>]", Caption.of("fawe.selection.sel.fuzzy-instruction"), "//sel fuzzy[=<mask>]");

                actor.print(box.create(1));
                return;
        }

        if (setDefaultSelector) {
            RegionSelectorType found = null;
            for (RegionSelectorType type : RegionSelectorType.values()) {
                if (type.getSelectorClass() == newSelector.getClass()) {
                    found = type;
                    break;
                }
            }

            if (found != null) {
                session.setDefaultRegionSelector(found);
                actor.print(Caption.of("worldedit.select.default-set", TextComponent.of(found.name())));
            } else {
                throw new RuntimeException("Something unexpected happened. Please report this.");
            }
        }

        session.setRegionSelector(world, newSelector);
        session.dispatchCUISelection(actor);
    }

    public static class BlockDistributionResult extends PaginationBox {

        private final List<Countable<BlockState>> distribution;
        private final int totalBlocks;
        private final boolean separateStates;

        public BlockDistributionResult(List<Countable<BlockState>> distribution, boolean separateStates) {
            this(distribution, separateStates, "//distr -p %page%" + (separateStates ? " -d" : ""));
        }

        public BlockDistributionResult(List<Countable<BlockState>> distribution, boolean separateStates, String pageCommand) {
            super("Block Distribution", pageCommand);
            this.distribution = distribution;
            // note: doing things like region.getArea is inaccurate for non-cuboids.
            this.totalBlocks = distribution.stream().mapToInt(Countable::getAmount).sum();
            this.separateStates = separateStates;
            setComponentsPerPage(7);
        }

        @Override
        public Component getComponent(int number) {
            Countable<BlockState> c = distribution.get(number);
            TextComponent.Builder line = TextComponent.builder();

            final int count = c.getAmount();

            final double perc = count / (double) totalBlocks * 100;
            final int maxDigits = (int) (Math.log10(totalBlocks) + 1);
            final int curDigits = (int) (Math.log10(count) + 1);
            line.append(String.format("%s%.3f%%  ", perc < 10 ? "  " : "", perc), TextColor.GOLD);
            final int space = maxDigits - curDigits;
            String pad = Strings.repeat(" ", space == 0 ? 2 : 2 * space + 1);
            line.append(String.format("%s%s", count, pad), TextColor.YELLOW);

            final BlockState state = c.getID();
            final BlockType blockType = state.getBlockType();
            Component blockName = blockType.getRichName();
            TextComponent toolTip;
            if (separateStates && state != blockType.getDefaultState()) {
                toolTip = TextComponent.of(state.getAsString());
                blockName = blockName.append(TextComponent.of("*"));
            } else {
                toolTip = TextComponent.of(blockType.getId());
            }
            blockName = blockName.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, toolTip));
            line.append(blockName);

            return line.build();
        }

        @Override
        public int getComponentsSize() {
            return distribution.size();
        }

        @Override
        public Component create(int page) throws InvalidComponentException {
            super.getContents().append(Caption.of("worldedit.distr.total", TextComponent.of(totalBlocks)))
                    .append(TextComponent.newline());
            return super.create(page);
        }
    }
}
