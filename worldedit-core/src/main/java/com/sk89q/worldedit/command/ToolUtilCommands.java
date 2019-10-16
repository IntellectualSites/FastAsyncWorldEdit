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
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.MathMan;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.util.HandSide;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

/**
 * Tool commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ToolUtilCommands {
    private final WorldEdit we;

    public ToolUtilCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
        name = "/",
        aliases = {","},
        desc = "Toggle the super pickaxe function"
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session,
                              @Arg(desc = "The new super pickaxe state", def = "")
                                  Boolean superPickaxe) {
        boolean hasSuperPickAxe = session.hasSuperPickAxe();
        if (superPickaxe != null && superPickaxe == hasSuperPickAxe) {
            player.printError("Super pickaxe already " + (superPickaxe ? "enabled" : "disabled") + ".");
                return;
            }

        if (hasSuperPickAxe) {
            session.disableSuperPickAxe();
            player.print("Super pickaxe disabled.");
        } else {
            session.enableSuperPickAxe();
            player.print("Super pickaxe enabled.");
        }
    }

    @Command(
            name = "mask",
            desc = "Set the brush destination mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void mask(Player player, LocalSession session,
                     @Arg(desc = "The destination mask", def = "")
                             Mask maskOpt,
                     Arguments arguments)
            throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (maskOpt == null) {
            player.print(BBC.BRUSH_MASK_DISABLED.s());
            tool.setMask(null);
            return;
        }
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        System.out.println(lastArg + " TODO check this is not the whole command");
        tool.setMask(maskOpt);
        player.print(BBC.BRUSH_MASK.s());
    }

    @Command(
            name = "material",
            aliases = {"mat", "/material", "pattern"},
            desc = "Set the brush material"
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, LocalSession session,
                         @Arg(desc = "The pattern of blocks to use")
                             Pattern pattern,
                         Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (pattern == null) {
            player.print(BBC.BRUSH_MATERIAL.s());
            tool.setFill(null);
            return;
        }
        tool.setFill(pattern);
        player.print(BBC.BRUSH_MATERIAL.s());
    }

    @Command(
            name = "range",
            desc = "Set the brush range"
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session,
                      @Arg(desc = "The range of the brush")
                              int range) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        tool.setRange(range);
        player.print(BBC.BRUSH_RANGE.s());
    }

    @Command(
            name = "size",
            desc = "Set the brush size"
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session,
                     @Arg(desc = "The size of the brush")
                             int size) throws WorldEditException {
        we.checkMaxBrushRadius(size);
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        tool.setSize(size);
        player.print(BBC.BRUSH_SIZE.s());
    }

    @Command(
        name = "tracemask",
        desc = "Set the mask used to stop tool traces"
    )
    @CommandPermissions("worldedit.brush.options.tracemask")
    public void traceMask(Player player, LocalSession session,
                          @Arg(desc = "The trace mask to set", def = "")
                             Mask maskOpt) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        tool.setTraceMask(maskOpt);
        BBC.BRUSH_TARGET_MASK_SET.send(player, maskOpt.toString());
    }

    @Command(
            name = "visualize",
            aliases = {"visual", "vis"},
            desc = "Toggle between different visualization modes",
            descFooter = "Toggle between different visualization modes\n" +
                    "0 = No visualization\n" +
                    "1 = Single block at target position\n" +
                    "2 = Glass showing what blocks will be changed"
    )
    @CommandPermissions("worldedit.brush.visualize")
    public void visual(Player player, LocalSession session,
        @Arg(name = "mode", desc = "int", def = "0")
            int mode) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        VisualMode[] modes = VisualMode.values();
        VisualMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setVisualMode(player, newMode);
        BBC.BRUSH_VISUAL_MODE_SET.send(player, newMode);
    }

    @Command(
            name = "target",
            aliases = {"tar"},
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.target")
    public void target(Player player, LocalSession session,
                       @Arg(name = "mode", desc = "int", def = "0")
                           int mode) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        BBC.BRUSH_TARGET_MODE_SET.send(player, newMode);
    }

    @Command(
            name = "targetoffset",
            aliases = {"to"},
            desc = "Set the targeting mask"
    )
    @CommandPermissions("worldedit.brush.targetoffset")
    public void targetOffset(Player player, EditSession editSession, LocalSession session,
                             int offset) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        tool.setTargetOffset(offset);
        BBC.BRUSH_TARGET_OFFSET_SET.send(player, offset);
    }


    @Command(
            name = "smask",
            aliases = {"/smask", "/sourcemask", "sourcemask"},
            desc = "Set the brush source mask",
            descFooter = "Set the brush source mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void smask(Player player, LocalSession session, EditSession editSession,
                      @Arg(desc = "The destination mask", def = "")
                              Mask maskArg,
                      Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (maskArg == null) {
            player.print(BBC.BRUSH_SOURCE_MASK_DISABLED.s());
            tool.setSourceMask(null);
            return;
        }
        tool.setSourceMask(maskArg);
        player.print(BBC.BRUSH_SOURCE_MASK.s());
    }

    @Command(
            name = "transform",
            desc = "Set the brush transform"
    )
    @CommandPermissions({"worldedit.brush.options.transform", "worldedit.transform.brush"})
    public void transform(Player player, LocalSession session, EditSession editSession,
                          @Arg(desc = "The transform", def = "")
                              ResettableExtent transform,
                          Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand(HandSide.MAIN_HAND).getType());
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.s());
            return;
        }
        if (transform == null) {
            player.print(BBC.BRUSH_TRANSFORM_DISABLED.s());
            tool.setTransform(null);
            return;
        }
        tool.setTransform(transform);
        player.print(BBC.BRUSH_TRANSFORM.s());
    }
}
