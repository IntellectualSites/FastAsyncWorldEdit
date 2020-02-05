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

import com.boydti.fawe.config.Caption;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.Scroll;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.util.HandSide;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;
import org.jetbrains.annotations.Range;

import java.util.List;

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
            name = "mask",
            desc = "Set the brush destination mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void mask(Player player, LocalSession session,
                     @Switch(name = 'h', desc = "TODO")
                             boolean offHand,
                     @Arg(desc = "The destination mask", def = "")
                             Mask maskOpt,
                     Arguments arguments)
            throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (maskOpt == null) {
            player.printInfo(TranslatableComponent.of("worldedit.tool.mask.disabled"));
            tool.setMask(null);
        } else {
            BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
            String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get()))
                .getSubstring();
            settings.addSetting(BrushSettings.SettingType.MASK, lastArg);
            settings.setMask(maskOpt);
            tool.update();
            player.printInfo(TranslatableComponent.of("worldedit.tool.mask.set"));
        }
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
                         @Switch(name = 'h', desc = "TODO")
                                 boolean offHand,
                         Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.printInfo(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
    }
        if (pattern == null) {
            tool.setFill(null);
        } else {
	        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
	        settings.setFill(pattern);
	        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
	        settings.addSetting(BrushSettings.SettingType.FILL, lastArg);
	        tool.update();
		}
		player.printInfo(TranslatableComponent.of("worldedit.tool.material.set"));
    }

    @Command(
        name = "range",
        desc = "Set the brush range"
        )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session,
                      @Arg(desc = "The range of the brush")
                          int range) throws WorldEditException {
        session.getBrushTool(player, false).setRange(range);
        player.printInfo(TranslatableComponent.of("worldedit.tool.range.set"));
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

        session.getBrushTool(player, false).setSize(size);
        player.printInfo(TranslatableComponent.of("worldedit.tool.size.set"));
    }
    
    @Command(
        name = "tracemask",
        aliases = {"tarmask", "tm", "targetmask"},
        desc = "Set the mask used to stop tool traces"
    )
    @CommandPermissions("worldedit.brush.options.tracemask")
    public void traceMask(Player player, LocalSession session,
                          @Arg(desc = "The trace mask to set", def = "")
                             Mask maskOpt) throws WorldEditException {
        session.getBrushTool(player, false).setTraceMask(maskOpt);
        if (maskOpt == null) {
            player.printInfo(TranslatableComponent.of("worldedit.tool.tracemask.disabled"));
        } else {
            player.printInfo(TranslatableComponent.of("worldedit.tool.tracemask.set"));
        }
    }

    @Command(
            name = "/superpickaxe",
            aliases = {",", "/sp", "/pickaxe"},
            desc = "Toggle the super pickaxe function"
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session,
                              @Arg(desc = "The new super pickaxe state", def = "")
                                  Boolean superPickaxe) {
        boolean hasSuperPickAxe = session.hasSuperPickAxe();
        if (superPickaxe != null && superPickaxe == hasSuperPickAxe) {
            player.printError(TranslatableComponent.of(superPickaxe ? "worldedit.tool.superpickaxe.enabled.already" : "worldedit.tool.superpickaxe.disabled.already"));
                return;
            }
        if (hasSuperPickAxe) {
            session.disableSuperPickAxe();
            player.printInfo(TranslatableComponent.of("worldedit.tool.superpickaxe.disabled"));
        } else {
            session.enableSuperPickAxe();
            player.printInfo(TranslatableComponent.of("worldedit.tool.superpickaxe.enabled"));
        }
    }

    @Command(
            name = "primary",
            desc = "Set the right click brush",
            descFooter = "Set the right click brush"
    )
    @CommandPermissions("worldedit.brush.primary")
    public void primary(Player player, LocalSession session,
                        @Arg(desc = "The brush command", variable = true) List<String> commandStr) throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(commandStr, " ");
        CommandEvent event = new CommandEvent(player, cmd);
        PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setSecondary(tool.getSecondary());
        }
    }

    @Command(
            name = "secondary",
            desc = "Set the left click brush",
            descFooter = "Set the left click brush"
    )
    @CommandPermissions("worldedit.brush.secondary")
    public void secondary(Player player, LocalSession session,
                          @Arg(desc = "The brush command", variable = true) List<String> commandStr)
            throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(commandStr, " ");
        CommandEvent event = new CommandEvent(player, cmd);
        PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setPrimary(tool.getPrimary());
        }
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
        @Arg(name = "mode", desc = "int", def = "0") @Range(from = 0, to = 2)
            int mode)
            throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        VisualMode[] modes = VisualMode.values();
        VisualMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setVisualMode(player, newMode);
        player.print(Caption.of("fawe.worldedit.brush.brush.visual.mode.set" , newMode));
    }

    @Command(
            name = "target",
            aliases = {"tar"},
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.target")
    public void target(Player player, LocalSession session,
                       @Arg(name = "mode", desc = "int", def = "0") int mode) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        player.print(Caption.of("fawe.worldedit.brush.brush.target.mode.set" , newMode));
    }

    @Command(
            name = "targetoffset",
            aliases = {"to"},
            desc = "Set the targeting mask"
    )
    @CommandPermissions("worldedit.brush.targetoffset")
    public void targetOffset(Player player, EditSession editSession, LocalSession session,
                             int offset) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        tool.setTargetOffset(offset);
        player.print(Caption.of("fawe.worldedit.brush.brush.target.offset.set" , offset));
    }

    @Command(
            name = "scroll",
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.scroll")
    public void scroll(Player player, EditSession editSession, LocalSession session,
                       @Switch(name = 'h', desc = "TODO")
                               boolean offHand,
                       @Arg(desc = "Target Modes", def = "none")
                               Scroll.Action mode,
                       @Arg(desc = "The scroll action", variable = true)
                               List<String> commandStr) throws WorldEditException {
        BrushTool bt = session.getBrushTool(player, false);
        if (bt == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }

        BrushSettings settings = offHand ? bt.getOffHand() : bt.getContext();
        Scroll action = Scroll.fromArguments(bt, player, session, mode, commandStr, true);
        settings.setScrollAction(action);
        if (mode == Scroll.Action.NONE) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.scroll.action.unset"));
        } else if (action != null) {
            String full = (mode.name().toLowerCase() + " " + StringMan.join(commandStr, " ")).trim();
            settings.addSetting(BrushSettings.SettingType.SCROLL_ACTION, full);
            player.print(Caption.of("fawe.worldedit.brush.brush.scroll.action.set" , mode));
        }
        bt.update();
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
                      @Switch(name = 'h', desc = "TODO")
                              boolean offHand,
                      Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (maskArg == null) {
            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.source.mask.disabled"));
            tool.setSourceMask(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.SOURCE_MASK, lastArg);
        settings.setSourceMask(maskArg);
        tool.update();
        player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.source.mask"));
    }

    // TODO: Ping @MattBDev to reimplement 2020-02-04
//    @Command(
//            name = "transform",
//            desc = "Set the brush transform"
//    )
//    @CommandPermissions({"worldedit.brush.options.transform", "worldedit.transform.brush"})
//    public void transform(Player player, LocalSession session, EditSession editSession,
//                          @Arg(desc = "The transform", def = "") ResettableExtent transform,
//                          @Switch(name = 'h', desc = "TODO")
//                                  boolean offHand,
//                          Arguments arguments) throws WorldEditException {
//        BrushTool tool = session.getBrushTool(player, false);
//        if (tool == null) {
//            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.none"));
//            return;
//        }
//        if (transform == null) {
//            player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.transform.disabled"));
//            tool.setTransform(null);
//            return;
//        }
//        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
//        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
//        settings.addSetting(BrushSettings.SettingType.TRANSFORM, lastArg);
//        settings.setTransform(transform);
//        tool.update();
//        player.print(TranslatableComponent.of("fawe.worldedit.brush.brush.transform"));
//    }
}
