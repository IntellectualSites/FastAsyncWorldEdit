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
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.InspectBrush;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.tool.BlockDataCyler;
import com.sk89q.worldedit.command.tool.BlockReplacer;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.DistanceWand;
import com.sk89q.worldedit.command.tool.FloatingTreeRemover;
import com.sk89q.worldedit.command.tool.FloodFillTool;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.command.tool.NavigationWand;
import com.sk89q.worldedit.command.tool.QueryTool;
import com.sk89q.worldedit.command.tool.SelectionWand;
import com.sk89q.worldedit.command.tool.TreePlanter;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.item.ItemType;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.util.List;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ToolCommands {
    private final WorldEdit we;

    public ToolCommands(WorldEdit we) {
        this.we = we;
    }

//    @Command(
//        name = "none",
//        desc = "Unbind a bound tool from your current item"
//    )
//    public void none(Player player, LocalSession session) throws WorldEditException {
//
//        session.setTool(player.getItemInHand(HandSide.MAIN_HAND).getType(), null);
//        player.print("Tool unbound from your current item.");
//    }

    @Command(
        name = "/selwand",
        aliases = "selwand",
        desc = "Selection wand tool"
    )
    @CommandPermissions("worldedit.setwand")
    public void selwand(Player player, LocalSession session) throws WorldEditException {
        final ItemType itemType = player.getItemInHand(HandSide.MAIN_HAND).getType();
        session.setTool(player, new SelectionWand());
        player.print("Selection wand bound to " + itemType.getName() + ".");
    }

    @Command(
        name = "/navwand",
        aliases = "navwand",
        desc = "Navigation wand tool"
    )
    @CommandPermissions("worldedit.setwand")
    public void navwand(Player player, LocalSession session) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new NavigationWand());
        player.print("Navigation wand bound to " + itemStack.getType().getName() + ".");
    }

    @Command(
        name = "info",
        desc = "Block information tool"
    )
    @CommandPermissions("worldedit.tool.info")
    public void info(Player player, LocalSession session) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new QueryTool());
        BBC.TOOL_INFO.send(player, itemStack.getType().getName());
    }

    @Command(
            name = "inspect",
            desc = "Inspect edits within a radius"
    )
    @CommandPermissions("worldedit.tool.inspect")
    public void inspectBrush(Player player, LocalSession session) throws WorldEditException {
        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new InspectBrush());
        BBC.TOOL_INSPECT.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "tree",
        desc = "Tree generator tool"
    )
    @CommandPermissions("worldedit.tool.tree")
    public void tree(Player player, LocalSession session,
                     @Arg(desc = "Type of tree to generate", def = "tree")
                     TreeGenerator.TreeType type) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new TreePlanter(type));
        BBC.TOOL_TREE.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "repl",
        desc = "Block replacer tool"
    )
    @CommandPermissions("worldedit.tool.replacer")
    public void repl(Player player, LocalSession session,
                     @Arg(desc = "The pattern of blocks to place")
                         Pattern pattern) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new BlockReplacer(pattern));
        BBC.TOOL_REPL.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "cycler",
        desc = "Block data cycler tool"
    )
    @CommandPermissions("worldedit.tool.data-cycler")
    public void cycler(Player player, LocalSession session) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new BlockDataCyler());
        BBC.TOOL_CYCLER.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "floodfill",
        aliases = { "flood" },
        desc = "Flood fill tool"
    )
    @CommandPermissions("worldedit.tool.flood-fill")
    public void floodFill(Player player, LocalSession session,
                          @Arg(desc = "The pattern to flood fill")
                              Pattern pattern,
                          @Arg(desc = "The range to perform the fill")
                              int range) throws WorldEditException {

        LocalConfiguration config = we.getConfiguration();

        if (range > config.maxSuperPickaxeSize) {
            BBC.TOOL_RANGE_ERROR.send(player, config.maxSuperPickaxeSize);
            return;
        }

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new FloodFillTool(range, pattern));
        BBC.TOOL_FLOOD_FILL.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "deltree",
        desc = "Floating tree remover tool"
    )
    @CommandPermissions("worldedit.tool.deltree")
    public void deltree(Player player, LocalSession session) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new FloatingTreeRemover());
        BBC.TOOL_DELTREE.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "farwand",
        desc = "Wand at a distance tool"
    )
    @CommandPermissions("worldedit.tool.farwand")
    public void farwand(Player player, LocalSession session) throws WorldEditException {

        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);
        session.setTool(player, new DistanceWand());
        BBC.TOOL_FARWAND.send(player, itemStack.getType().getName());
    }

    @Command(
        name = "lrbuild",
        aliases = { "/lrbuild" },
        desc = "Long-range building tool"
    )
    @CommandPermissions("worldedit.tool.lrbuild")
    public void longrangebuildtool(Player player, LocalSession session,
        @Arg(desc = "Pattern to set on left-click")
            Pattern primary,
        @Arg(desc = "Pattern to set on right-click")
            Pattern secondary) throws WorldEditException {
        BaseItemStack itemStack = player.getItemInHand(HandSide.MAIN_HAND);

        session.setTool(player, new LongRangeBuildTool(primary, secondary));
        BBC.TOOL_LRBUILD_BOUND.send(player, itemStack.getType().getName());
        BBC.TOOL_LRBUILD_INFO.send(player, secondary, primary);
    }
}
