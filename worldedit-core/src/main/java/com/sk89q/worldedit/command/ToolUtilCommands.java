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

import com.fastasyncworldedit.core.command.tool.TargetMode;
import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.fastasyncworldedit.core.command.tool.scroll.Scroll;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.util.MathMan;
import com.fastasyncworldedit.core.util.StringMan;
import com.google.common.collect.Iterables;
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

import java.util.List;
import java.util.Locale;

/**
 * Tool commands.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ToolUtilCommands {

    private final WorldEdit we;

    public ToolUtilCommands(WorldEdit we) {
        this.we = we;
    }

    //FAWE start - destination mask > mask
    @Command(
            name = "mask",
            aliases = "/mask",
            desc = "Set the brush destination mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void mask(
            Player player, LocalSession session,
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary,
            @Arg(desc = "The destination mask", def = "")
                    Mask maskOpt, Arguments arguments
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (maskOpt == null) {
            player.print(Caption.of("worldedit.tool.mask.disabled"));
            tool.setMask(null);
        } else {
            BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
            String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
            settings.addSetting(BrushSettings.SettingType.MASK, lastArg);
            settings.setMask(maskOpt);
            tool.update();
            player.print(Caption.of("worldedit.tool.mask.set"));
        }
    }
    //FAWE end

    @Command(
            name = "material",
            aliases = {"mat", "/material", "pattern"},
            desc = "Set the brush material"
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(
            Player player, LocalSession session,
            @Arg(desc = "The pattern of blocks to use")
                    Pattern pattern,
            //FAWE start - add secondary
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary, Arguments arguments
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (pattern == null) {
            tool.setFill(null);
        } else {
            BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
            settings.setFill(pattern);
            String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
            settings.addSetting(BrushSettings.SettingType.FILL, lastArg);
            tool.update();
        }
        //FAWE end
        player.print(Caption.of("worldedit.tool.material.set"));
    }

    @Command(
            name = "range",
            aliases = "/range",
            desc = "Set the brush range"
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(
            Player player, LocalSession session,
            @Arg(desc = "The range of the brush")
                    int range
    ) throws WorldEditException {
        session.getBrushTool(player).setRange(range);
        player.print(Caption.of("worldedit.tool.range.set"));
    }

    @Command(
            name = "size",
            desc = "Set the brush size"
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(
            Player player, LocalSession session,
            @Arg(desc = "The size of the brush")
                    int size,
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary, Arguments arguments
    ) throws WorldEditException {
        we.checkMaxBrushRadius(size);
        BrushTool tool = session.getBrushTool(player, false);

        BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
        settings.setSize(size);
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.FILL, lastArg);
        tool.update();
        player.print(Caption.of("worldedit.tool.size.set"));
    }

    @Command(
            name = "tracemask",
            aliases = {"tarmask", "tm", "targetmask"},
            desc = "Set the mask used to stop tool traces"
    )
    @CommandPermissions("worldedit.brush.options.tracemask")
    public void traceMask(
            Player player, LocalSession session,
            @Arg(desc = "The trace mask to set", def = "")
            Mask maskOpt
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("worldedit.brush.none.equipped"));
            return;
        }
        tool.setTraceMask(maskOpt);
        if (maskOpt == null) {
            player.print(Caption.of("worldedit.tool.tracemask.disabled"));
        } else {
            player.print(Caption.of("worldedit.tool.tracemask.set"));
        }
    }

    @Command(
            name = "/",
            aliases = {","},
            desc = "Toggle the super pickaxe function"
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(
            Player player, LocalSession session,
            @Arg(desc = "The new super pickaxe state", def = "")
                    Boolean superPickaxe
    ) {
        boolean hasSuperPickAxe = session.hasSuperPickAxe();
        if (superPickaxe != null && superPickaxe == hasSuperPickAxe) {
            player.print(Caption.of(superPickaxe
                    ? "worldedit.tool.superpickaxe.enabled.already"
                    : "worldedit.tool.superpickaxe.disabled.already"));
            return;
        }
        if (hasSuperPickAxe) {
            session.disableSuperPickAxe();
            player.print(Caption.of("worldedit.tool.superpickaxe.disabled"));
        } else {
            session.enableSuperPickAxe();
            player.print(Caption.of("worldedit.tool.superpickaxe.enabled"));
        }
    }

    //FAWE start
    @Command(
            name = "primary",
            aliases = {"/primary"},
            desc = "Set the right click brush",
            descFooter = "Set the right click brush"
    )
    @CommandPermissions("worldedit.brush.primary")
    public void primary(
            Player player, LocalSession session,
            @Arg(desc = "The brush command", variable = true) List<String> commandStr
    ) throws WorldEditException {
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
            aliases = {"/secondary"},
            desc = "Set the left click brush",
            descFooter = "Set the left click brush"
    )
    @CommandPermissions("worldedit.brush.secondary")
    public void secondary(
            Player player, LocalSession session,
            @Arg(desc = "The brush command", variable = true)
                    List<String> commandStr
    ) throws WorldEditException {
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
            name = "target",
            aliases = {"tar", "/target", "/tar"},
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.target")
    public void target(
            Player player, LocalSession session,
            @Arg(name = "mode", desc = "int", def = "0")
                    int mode
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        player.print(Caption.of("fawe.worldedit.brush.brush.target.mode.set", newMode));
    }

    @Command(
            name = "targetoffset",
            aliases = {"to"},
            desc = "Set the targeting mask"
    )
    @CommandPermissions("worldedit.brush.targetoffset")
    public void targetOffset(
            Player player, LocalSession session,
            @Arg(name = "offset", desc = "offset", def = "0") int offset
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        tool.setTargetOffset(offset);
        player.print(Caption.of("fawe.worldedit.brush.brush.target.offset.set", offset));
    }

    @Command(
            name = "scroll",
            desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.scroll")
    public void scroll(
            Player player, LocalSession session,
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary,
            @Arg(desc = "Target Modes", def = "none")
                    Scroll.Action mode,
            @Arg(desc = "The scroll action", variable = true)
                    List<String> commandStr
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }

        BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
        Scroll action = Scroll.fromArguments(tool, player, session, mode, commandStr, true);
        settings.setScrollAction(action);
        if (mode == Scroll.Action.NONE) {
            player.print(Caption.of("fawe.worldedit.brush.brush.scroll.action.unset"));
        } else if (action != null) {
            String full = (mode.name().toLowerCase(Locale.ROOT) + " " + StringMan.join(commandStr, " ")).trim();
            settings.addSetting(BrushSettings.SettingType.SCROLL_ACTION, full);
            player.print(Caption.of("fawe.worldedit.brush.brush.scroll.action.set", mode));
        }
        tool.update();
    }

    @Command(
            name = "smask",
            aliases = {"/smask", "/sourcemask", "sourcemask"},
            desc = "Set the brush source mask",
            descFooter = "Set the brush source mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void smask(
            Player player, LocalSession session,
            @Arg(desc = "The destination mask", def = "")
                    Mask maskArg,
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary,
            Arguments arguments
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (maskArg == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.source.mask.disabled"));
            tool.setSourceMask(null);
            return;
        }
        BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.SOURCE_MASK, lastArg);
        settings.setSourceMask(maskArg);
        tool.update();
        player.print(Caption.of("fawe.worldedit.brush.brush.source.mask"));
    }

    @Command(
            name = "transform",
            aliases = {"/transform"},
            desc = "Set the brush transform"
    )
    @CommandPermissions({"worldedit.brush.options.transform", "worldedit.transform.brush"})
    public void transform(
            Player player, LocalSession session,
            @Arg(desc = "The transform", def = "") ResettableExtent transform,
            @Switch(name = 'h', desc = "Modifies the secondary brush")
                    boolean secondary,
            Arguments arguments
    ) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.none"));
            return;
        }
        if (transform == null) {
            player.print(Caption.of("fawe.worldedit.brush.brush.transform.disabled"));
            tool.setTransform(null);
            return;
        }
        BrushSettings settings = secondary ? tool.getSecondary() : tool.getPrimary();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.TRANSFORM, lastArg);
        settings.setTransform(transform);
        tool.update();
        player.print(Caption.of("fawe.worldedit.brush.brush.transform"));
    }
    //FAWE end
}
