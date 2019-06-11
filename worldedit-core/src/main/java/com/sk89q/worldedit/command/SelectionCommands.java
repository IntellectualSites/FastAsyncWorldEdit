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

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.clipboard.URIClipboardHolder;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.regions.selector.FuzzyRegionSelector;
import com.boydti.fawe.object.regions.selector.PolyhedralRegionSelector;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
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
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.item.ItemTypes;
import com.sk89q.worldedit.world.storage.ChunkStore;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static com.sk89q.minecraft.util.commands.Logging.LogMode.POSITION;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Selection commands.
 */
@Command(aliases = {}, desc = "Change your selection points, mode or view info about your selection: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Selection)")
public class SelectionCommands {

    private final WorldEdit we;

    public SelectionCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        aliases = { "/pos1", "posa", "/1" },
        usage = "[coordinates]",
        desc = "Set position 1",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos1(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        BlockVector3 pos;

        if (args.argsLength() == 1) {
            if (args.getString(0).matches("-?\\d+,-?\\d+,-?\\d+")) {
                String[] coords = args.getString(0).split(",");
                pos = BlockVector3.at(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            } else {
                BBC.SELECTOR_INVALID_COORDINATES.send(player, args.getString(0));
                return;
            }
        } else {
            pos = player.getBlockIn().toBlockPoint();
        }
        pos = pos.clampY(0, player.getWorld().getMaximumPoint().getBlockY());
        if (!session.getRegionSelector(player.getWorld()).selectPrimary(pos, ActorSelectorLimits.forActor(player))) {
            BBC.SELECTOR_ALREADY_SET.send(player);
            return;
        }

        session.getRegionSelector(player.getWorld())
               .explainPrimarySelection(player, session, pos);
    }

    @Command(
        aliases = { "/pos2", "posb", "/2" },
        usage = "[coordinates]",
        desc = "Set position 2",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.pos")
    public void pos2(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        BlockVector3 pos;
        if (args.argsLength() == 1) {
            if (args.getString(0).matches("-?\\d+,-?\\d+,-?\\d+")) {
                String[] coords = args.getString(0).split(",");
                pos = BlockVector3.at(Integer.parseInt(coords[0]),
                        Integer.parseInt(coords[1]),
                        Integer.parseInt(coords[2]));
            } else {
                BBC.SELECTOR_INVALID_COORDINATES.send(player, args.getString(0));
                return;
            }
        } else {
            pos = player.getBlockIn().toBlockPoint();
        }
        pos = pos.clampY(0, player.getWorld().getMaximumPoint().getBlockY());
        if (!session.getRegionSelector(player.getWorld()).selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
            BBC.SELECTOR_ALREADY_SET.send(player);
            return;
        }

        session.getRegionSelector(player.getWorld())
                .explainSecondarySelection(player, session, pos);
    }

    @Command(
        aliases = { "/hpos1" },
        usage = "",
        desc = "Set position 1 to targeted block",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos1(Player player, LocalSession session) throws WorldEditException {

        Location pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectPrimary(pos.toVector().toBlockPoint(), ActorSelectorLimits.forActor(player))) {
                BBC.SELECTOR_ALREADY_SET.send(player);
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainPrimarySelection(player, session, pos.toVector().toBlockPoint());
        } else {
            BBC.NO_BLOCK.send(player);
        }
    }

    @Command(
        aliases = { "/hpos2" },
        usage = "",
        desc = "Set position 2 to targeted block",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.hpos")
    public void hpos2(Player player, LocalSession session) throws WorldEditException {

    	BlockVector3 pos = player.getBlockTrace(300).toBlockPoint();

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld()).selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
                BBC.SELECTOR_ALREADY_SET.send(player);
                return;
            }

            session.getRegionSelector(player.getWorld())
                    .explainSecondarySelection(player, session, pos);
        } else {
            BBC.NO_BLOCK.send(player);
        }
    }

    @Command(
        aliases = { "/chunk" },
        usage = "[x,z coordinates]",
        flags = "sc",
        desc = "Set the selection to your current chunk.",
        help =
            "Set the selection to the chunk you are currently in.\n" +
            "With the -s flag, your current selection is expanded\n" +
            "to encompass all chunks that are part of it.\n\n" +
            "Specifying coordinates will use those instead of your\n"+
            "current position. Use -c to specify chunk coordinates,\n" +
            "otherwise full coordinates will be implied.\n" +
            "(for example, the coordinates 5,5 are the same as -c 0,0)",
        min = 0,
        max = 1
    )
    @Logging(POSITION)
    @CommandPermissions("worldedit.selection.chunk")
    public void chunk(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        final BlockVector3 min;
        final BlockVector3 max;
        final World world = player.getWorld();
        if (args.hasFlag('s')) {
            Region region = session.getSelection(world);

            final BlockVector2 min2D = ChunkStore.toChunk(region.getMinimumPoint());
            final BlockVector2 max2D = ChunkStore.toChunk(region.getMaximumPoint());

            min = BlockVector3.at(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = BlockVector3.at(max2D.getBlockX() * 16 + 15, world.getMaxY(), max2D.getBlockZ() * 16 + 15);

            BBC.SELECTION_CHUNKS.send(player, min2D.getBlockX() + ", " + min2D.getBlockZ(), max2D.getBlockX() + ", " + max2D.getBlockZ());
        } else {
            final BlockVector2 min2D;
            if (args.argsLength() == 1) {
                // coords specified
                String[] coords = args.getString(0).split(",");
                if (coords.length != 2) {
                    BBC.SELECTOR_INVALID_COORDINATES.send(player, args.getString(0));
                }
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                BlockVector2 pos = BlockVector2.at(x, z);
                min2D = (args.hasFlag('c')) ? pos : ChunkStore.toChunk(pos.toBlockVector3());
            } else {
                // use player loc
                min2D = ChunkStore.toChunk(player.getBlockIn().toBlockPoint());
            }

            min = BlockVector3.at(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = min.add(15, world.getMaxY(), 15);

            BBC.SELECTION_CHUNK.send(player, min2D.getBlockX() + ", " + min2D.getBlockZ());
        }

        final CuboidRegionSelector selector;
        if (session.getRegionSelector(world) instanceof ExtendingCuboidRegionSelector) {
            selector = new ExtendingCuboidRegionSelector(world);
        } else {
            selector = new CuboidRegionSelector(world);
        }
        selector.selectPrimary(min, ActorSelectorLimits.forActor(player));
        selector.selectSecondary(max, ActorSelectorLimits.forActor(player));
        session.setRegionSelector(world, selector);

        session.dispatchCUISelection(player);

    }

    @Command(
        aliases = { "/wand" },
        usage = "",
        desc = "Get the wand object",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.wand")
    public void wand(Player player) throws WorldEditException {

        player.giveItem(new BaseItemStack(ItemTypes.parse(we.getConfiguration().wandItem), 1));
        BBC.SELECTION_WAND.send(player);
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips"))
            BBC.TIP_SEL_LIST.or(BBC.TIP_SELECT_CONNECTED, BBC.TIP_SET_POS1, BBC.TIP_FARWAND, BBC.TIP_DISCORD).send(player);
    }

    @Command(
        aliases = { "toggleeditwand" },
        usage = "",
        desc = "Toggle functionality of the edit wand",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.wand.toggle")
    public void toggleWand(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        session.setToolControl(!session.isToolControlEnabled());

        if (session.isToolControlEnabled()) {
            BBC.SELECTION_WAND_ENABLE.m().send(player);
        } else {
            BBC.SELECTION_WAND_DISABLE.send(player);
        }
    }

    @Command(
        aliases = { "/expand" },
        usage = "<amount> [reverse-amount] <direction>",
        desc = "Expand the selection area",
        min = 1,
        max = 3
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.expand")
    public void expand(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        // Special syntax (//expand vert) to expand the selection between
        // sky and bedrock.
        if (args.getString(0).equalsIgnoreCase("vert")
                || args.getString(0).equalsIgnoreCase("vertical")) {
            Region region = session.getSelection(player.getWorld());
            try {
                int oldSize = region.getArea();
                region.expand(
                        BlockVector3.at(0, (player.getWorld().getMaxY() + 1), 0),
                        BlockVector3.at(0, -(player.getWorld().getMaxY() + 1), 0));
                session.getRegionSelector(player.getWorld()).learnChanges();
                int newSize = region.getArea();
                session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
                BBC.SELECTION_EXPAND_VERT.send(player, (newSize - oldSize));
            } catch (RegionOperationException e) {
                player.printError(e.getMessage());
            }

            return;
        }

        List<BlockVector3> dirs = new ArrayList<>();
        int change = args.getInteger(0);
        int reverseChange = 0;

        switch (args.argsLength()) {
            case 2:
                // Either a reverse amount or a direction
                try {
                    reverseChange = args.getInteger(1);
                    dirs.add(we.getDirection(player, "me"));
                } catch (NumberFormatException e) {
                    if (args.getString(1).contains(",")) {
                        String[] split = args.getString(1).split(",");
                        for (String s : split) {
                            dirs.add(we.getDirection(player, s.toLowerCase()));
                        }
                    } else {
                        dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
                    }
                }
                break;

            case 3:
                // Both reverse amount and direction
                reverseChange = args.getInteger(1);
                if (args.getString(2).contains(",")) {
                    String[] split = args.getString(2).split(",");
                    for (String s : split) {
                        dirs.add(we.getDirection(player, s.toLowerCase()));
                    }
                } else {
                    dirs.add(we.getDirection(player, args.getString(2).toLowerCase()));
                }
                break;

            default:
                dirs.add(we.getDirection(player, "me"));
                break;

        }

        Region region = session.getSelection(player.getWorld());
        int oldSize = region.getArea();

        if (reverseChange == 0) {
            for (BlockVector3 dir : dirs) {
                region.expand(dir.multiply(change));
            }
        } else {
            for (BlockVector3 dir : dirs) {
                region.expand(dir.multiply(change), dir.multiply(-reverseChange));
            }
        }

        session.getRegionSelector(player.getWorld()).learnChanges();
        int newSize = region.getArea();

        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);

        BBC.SELECTION_EXPAND.send(player, (newSize - oldSize));
    }

    @Command(
        aliases = { "/contract" },
        usage = "<amount> [reverse-amount] [direction]",
        desc = "Contract the selection area",
        min = 1,
        max = 3
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.contract")
    public void contract(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        List<BlockVector3> dirs = new ArrayList<>();
        int change = args.getInteger(0);
        int reverseChange = 0;

        switch (args.argsLength()) {
            case 2:
                // Either a reverse amount or a direction
                try {
                    reverseChange = args.getInteger(1);
                    dirs.add(we.getDirection(player, "me"));
                } catch (NumberFormatException e) {
                    if (args.getString(1).contains(",")) {
                        String[] split = args.getString(1).split(",");
                        for (String s : split) {
                            dirs.add(we.getDirection(player, s.toLowerCase()));
                        }
                    } else {
                        dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
                    }
                }
                break;

            case 3:
                // Both reverse amount and direction
                reverseChange = args.getInteger(1);
                if (args.getString(2).contains(",")) {
                    String[] split = args.getString(2).split(",");
                    for (String s : split) {
                        dirs.add(we.getDirection(player, s.toLowerCase()));
                    }
                } else {
                    dirs.add(we.getDirection(player, args.getString(2).toLowerCase()));
                }
                break;

            default:
                dirs.add(we.getDirection(player, "me"));
                break;
        }

        try {
            Region region = session.getSelection(player.getWorld());
            int oldSize = region.getArea();
            if (reverseChange == 0) {
                for (BlockVector3 dir : dirs) {
                    region.contract(dir.multiply(change));
                }
            } else {
                for (BlockVector3 dir : dirs) {
                    region.contract(dir.multiply(change), dir.multiply(-reverseChange));
                }
            }
            session.getRegionSelector(player.getWorld()).learnChanges();
            int newSize = region.getArea();

            session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);

            BBC.SELECTION_CONTRACT.send(player, (oldSize - newSize));
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    @Command(
        aliases = { "/shift" },
        usage = "<amount> [direction]",
        desc = "Shift the selection area",
        min = 1,
        max = 2
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.shift")
    public void shift(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        List<BlockVector3> dirs = new ArrayList<>();
        int change = args.getInteger(0);
        if (args.argsLength() == 2) {
            if (args.getString(1).contains(",")) {
                for (String s : args.getString(1).split(",")) {
                    dirs.add(we.getDirection(player, s.toLowerCase()));
                }
            } else {
                dirs.add(we.getDirection(player, args.getString(1).toLowerCase()));
            }
        } else {
            dirs.add(we.getDirection(player, "me"));
        }

        try {
            Region region = session.getSelection(player.getWorld());

            for (BlockVector3 dir : dirs) {
                region.shift(dir.multiply(change));
            }

            session.getRegionSelector(player.getWorld()).learnChanges();

            session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);

            BBC.SELECTION_SHIFT.send(player);
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    @Command(
        aliases = { "/outset" },
        usage = "<amount>",
        desc = "Outset the selection area",
        help =
            "Expands the selection by the given amount in all directions.\n" +
            "Flags:\n" +
            "  -h only expand horizontally\n" +
            "  -v only expand vertically\n",
        flags = "hv",
        min = 1,
        max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.outset")
    public void outset(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        Region region = session.getSelection(player.getWorld());
        region.expand(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld()).learnChanges();
        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
        BBC.SELECTION_OUTSET.send(player);
    }

    @Command(
        aliases = { "/inset" },
        usage = "<amount>",
        desc = "Inset the selection area",
        help =
            "Contracts the selection by the given amount in all directions.\n" +
            "Flags:\n" +
            "  -h only contract horizontally\n" +
            "  -v only contract vertically\n",
        flags = "hv",
        min = 1,
        max = 1
    )
    @Logging(REGION)
    @CommandPermissions("worldedit.selection.inset")
    public void inset(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        Region region = session.getSelection(player.getWorld());
        region.contract(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld()).learnChanges();
        session.getRegionSelector(player.getWorld()).explainRegionAdjust(player, session);
        BBC.SELECTION_INSET.send(player);
    }

    private BlockVector3[] getChangesForEachDir(CommandContext args) {
        List<BlockVector3> changes = new ArrayList<>(6);
        int change = args.getInteger(0);

        if (!args.hasFlag('h')) {
            changes.add((BlockVector3.UNIT_Y).multiply(change));
            changes.add((BlockVector3.UNIT_MINUS_Y).multiply(change));
        }

        if (!args.hasFlag('v')) {
            changes.add((BlockVector3.UNIT_X).multiply(change));
            changes.add((BlockVector3.UNIT_MINUS_X).multiply(change));
            changes.add((BlockVector3.UNIT_Z).multiply(change));
            changes.add((BlockVector3.UNIT_MINUS_Z).multiply(change));
        }

        return changes.toArray(new BlockVector3[0]);
    }

    @Command(
        aliases = { "/size" },
        flags = "c",
        usage = "",
        desc = "Get information about the selection",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.selection.size")
    public void size(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        if (args.hasFlag('c')) {
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

                Region region = clipboard.getRegion();
                BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(BlockVector3.ONE);
                BlockVector3 origin = clipboard.getOrigin();

                String sizeStr = size.getBlockX() + "*" + size.getBlockY() + "*" + size.getBlockZ();
                String originStr = origin.getBlockX() + "," + origin.getBlockY() + "," + origin.getBlockZ();

                long numBlocks = ((long) size.getBlockX() * size.getBlockY() * size.getBlockZ());

                String msg = String.format("%1$s: %2$s @ %3$s (%4$d blocks)", name, sizeStr, originStr, numBlocks);
                player.print(msg);

                index++;
            }



//            player.print("Cuboid dimensions (max - min): " + size);
//            player.print("Offset: " + origin);
//            player.print("Cuboid distance: " + size.distance(Vector.ONE));
//            player.print("# of blocks: " + (int) (size.getX() * size.getY() * size.getZ()));
//=======
//    public void size(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
//        if (args.hasFlag('c')) {
//            ClipboardHolder holder = session.getClipboard();
//            Clipboard clipboard = holder.getClipboard();
//            Region region = clipboard.getRegion();
//            BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint());
//            BlockVector3 origin = clipboard.getOrigin();
//
//            player.print("Cuboid dimensions (max - min): " + size);
//            player.print("Offset: " + origin);
//            player.print("Cuboid distance: " + size.distance(BlockVector3.ONE));
//            player.print("# of blocks: " + (int) (size.getX() * size.getY() * size.getZ()));
            return;
        }

        Region region = session.getSelection(player.getWorld());
        BlockVector3 size = region.getMaximumPoint()
                .subtract(region.getMinimumPoint())
                .add(1, 1, 1);

        player.print("Type: " + session.getRegionSelector(player.getWorld())
                .getTypeName());

        for (String line : session.getRegionSelector(player.getWorld())
                .getInformationLines()) {
            player.print(line);
        }

        player.print("Size: " + size);
        player.print("Cuboid distance: " + region.getMaximumPoint().distance(region.getMinimumPoint()));
        player.print("# of blocks: " + region.getArea());
    }


    @Command(
        aliases = { "/count" },
        usage = "<mask>",
        flags = "d",
        desc = "Counts the number of a certain type of block",
        min = 1,
        max = 1
    )
    @CommandPermissions("worldedit.analysis.count")
    public void count(Player player, LocalSession session, EditSession editSession, Mask mask) throws WorldEditException {
        int count = editSession.countBlock(session.getSelection(player.getWorld()), mask);
        BBC.SELECTION_COUNT.send(player, count);
    }

    @Command(
        aliases = { "/distr" },
        usage = "",
        desc = "Get the distribution of blocks in the selection",
        help =
            "Gets the distribution of blocks in the selection.\n" +
            "The -c flag gets the distribution of your clipboard.\n" +
            "The -d flag separates blocks by state",
        flags = "cd",
        min = 0,
        max = 0
    )
    @CommandPermissions("worldedit.analysis.distr")
    public void distr(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException, CommandException {
        int size;
        List<Countable> distributionData;
        boolean separateStates = args.hasFlag('d');

        Region region;
        if (args.hasFlag('c')) {
            // TODO multi clipboard distribution
            Clipboard clipboard = session.getClipboard().getClipboard();
            region = clipboard.getRegion();
            editSession.setExtent(new AbstractDelegateExtent(clipboard));
        } else {
            region = session.getSelection(player.getWorld());
        }
        if (separateStates) {
            distributionData = (List) editSession.getBlockDistributionWithData(region);
        } else {
            distributionData = (List) editSession.getBlockDistribution(region);
        }
        size = session.getSelection(player.getWorld()).getArea();

        if (distributionData.isEmpty()) {  // *Should* always be false
            player.printError("No blocks counted.");
            return;
        }
        BBC.SELECTION_DISTR.send(player, size);

        for (Countable c : distributionData) {
            String name = c.getID().toString();
            String str = String.format("%-7s (%.3f%%) %s",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    name);
            player.print(str);
        }
    }

    @Command(
        aliases = { "/sel", ";", "/desel", "/deselect" },
        flags = "d",
        usage = "[cuboid|extend|poly|ellipsoid|sphere|cyl|convex]",
        desc = "Choose a region selector",
        min = 0,
        max = 1
    )
    public void select(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException {
        final World world = player.getWorld();
        if (args.argsLength() == 0) {
            session.getRegionSelector(world).clear();
            session.dispatchCUISelection(player);
            BBC.SELECTION_CLEARED.send(player);
            return;
        }

        final String typeName = args.getString(0);
        final RegionSelector oldSelector = session.getRegionSelector(world);

        final RegionSelector selector;
        if (typeName.equalsIgnoreCase("cuboid")) {
            selector = new CuboidRegionSelector(oldSelector);
            player.print(BBC.SEL_CUBOID.s());
        } else if (typeName.equalsIgnoreCase("extend")) {
            selector = new ExtendingCuboidRegionSelector(oldSelector);
            player.print(BBC.SEL_CUBOID_EXTEND.s());
        } else if (typeName.equalsIgnoreCase("poly")) {
            selector = new Polygonal2DRegionSelector(oldSelector);
            player.print(BBC.SEL_2D_POLYGON.s());
            Optional<Integer> limit = ActorSelectorLimits.forActor(player).getPolygonVertexLimit();
            limit.ifPresent(integer -> player.print(BBC.SEL_MAX.f(integer)));
            player.print(BBC.SEL_LIST.s());
        } else if (typeName.equalsIgnoreCase("ellipsoid")) {
            selector = new EllipsoidRegionSelector(oldSelector);
            player.print(BBC.SEL_ELLIPSIOD.s());
        } else if (typeName.equalsIgnoreCase("sphere")) {
            selector = new SphereRegionSelector(oldSelector);
            player.print(BBC.SEL_SPHERE.s());
        } else if (typeName.equalsIgnoreCase("cyl")) {
            selector = new CylinderRegionSelector(oldSelector);
            player.print(BBC.SEL_CYLINDRICAL.s());
        } else if (typeName.equalsIgnoreCase("convex") || typeName.equalsIgnoreCase("hull")) {
            selector = new ConvexPolyhedralRegionSelector(oldSelector);
            player.print(BBC.SEL_CONVEX_POLYHEDRAL.s());
            Optional<Integer> limit = ActorSelectorLimits.forActor(player).getPolyhedronVertexLimit();
            limit.ifPresent(integer -> player.print(BBC.SEL_MAX.f(integer)));
            player.print(BBC.SEL_LIST.s());
        } else if (typeName.equalsIgnoreCase("polyhedral") || typeName.equalsIgnoreCase("polyhedron")) {
            selector = new PolyhedralRegionSelector(player.getWorld());
            player.print(BBC.SEL_CONVEX_POLYHEDRAL.s());
            Optional<Integer> limit = ActorSelectorLimits.forActor(player).getPolyhedronVertexLimit();
            limit.ifPresent(integer -> player.print(BBC.SEL_MAX.f(integer)));
            player.print(BBC.SEL_LIST.s());
        } else if (typeName.startsWith("fuzzy") || typeName.startsWith("magic")) {
            Mask mask;
            if (typeName.length() > 6) {
                ParserContext parserContext = new ParserContext();
                parserContext.setActor(player);
                parserContext.setWorld(player.getWorld());
                parserContext.setSession(session);
                parserContext.setExtent(editSession);
                mask = we.getMaskFactory().parseFromInput(typeName.substring(6), parserContext);
            } else {
                mask = new IdMask(editSession);
            }
            selector = new FuzzyRegionSelector(player, editSession, mask);
            player.print(BBC.SEL_FUZZY.f());
            player.print(BBC.SEL_LIST.f());
        } else {
            CommandListBox box = new CommandListBox("Selection modes");
            StyledFragment contents = box.getContents();
            StyledFragment tip = contents.createFragment(Style.RED);
            tip.append(BBC.SEL_MODES.s()).newLine();

            box.appendCommand("//sel cuboid", "Select two corners of a cuboid");
            box.appendCommand("//sel extend", "Fast cuboid selection mode");
            box.appendCommand("//sel poly", "Select a 2D polygon with height");
            box.appendCommand("//sel ellipsoid", "Select an ellipsoid");
            box.appendCommand("//sel sphere", "Select a sphere");
            box.appendCommand("//sel cyl", "Select a cylinder");
            box.appendCommand("//sel convex", "Select a convex polyhedral");
            box.appendCommand("//sel polyhedral", "Select a hollow polyhedral");
            box.appendCommand("//sel fuzzy[=<mask>]", "Select all connected blocks (magic wand)");

            player.printRaw(ColorCodeBuilder.asColorCodes(box));
            return;
        }

        if (args.hasFlag('d')) {
            RegionSelectorType found = null;
            for (RegionSelectorType type : RegionSelectorType.values()) {
                if (type.getSelectorClass() == selector.getClass()) {
                    found = type;
                    break;
                }
            }

            if (found != null) {
                session.setDefaultRegionSelector(found);
                BBC.SELECTOR_SET_DEFAULT.send(player, found.name());
            } else {
                throw new RuntimeException("Something unexpected happened. Please report this.");
            }
        }

        session.setRegionSelector(world, selector);
        session.dispatchCUISelection(player);
    }

}
