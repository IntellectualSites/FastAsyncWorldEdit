package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.internal.annotation.Range;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Tool commands.
 */

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class BrushOptionsCommands {

    private WorldEdit worldEdit;

    public BrushOptionsCommands(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "savebrush",
        aliases = {"save"},
        desc = "Save your current brush",
        descFooter = "Save your current brush use the -g flag to save globally"
    )
    @CommandPermissions("worldedit.brush.save")
    public void saveBrush(Player player, LocalSession session, String name,
        @Switch(name = 'g', desc = "TODO") boolean root) throws WorldEditException, IOException {
        BrushTool tool = session.getBrushTool(player);
        if (tool != null) {
            root |= name.startsWith("../");
            name = FileSystems.getDefault().getPath(name).getFileName().toString();
            File folder = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
            name = name.endsWith(".jsgz") ? name : name + ".jsgz";
            File file;
            if (root && player.hasPermission("worldedit.brush.save.other")) {
                file = new File(folder, name);
            } else {
                file = new File(folder, player.getUniqueId() + File.separator + name);
            }
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
            try (DataOutputStream out = new DataOutputStream(
                new PGZIPOutputStream(new FileOutputStream(file)))) {
                out.writeUTF(tool.toString());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            BBC.SCHEMATIC_SAVED.send(player, name);
        } else {
            BBC.BRUSH_NONE.send(player);
        }
    }

    @Command(
        name = "loadbrush",
        aliases = {"load"},
        desc = "Load a brush"
    )
    @CommandPermissions("worldedit.brush.load")
    public void loadBrush(Player player, LocalSession session, String name)
        throws WorldEditException, IOException {
        name = FileSystems.getDefault().getPath(name).getFileName().toString();
        File folder = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
        name = name.endsWith(".jsgz") ? name : name + ".jsgz";
        File file = new File(folder, player.getUniqueId() + File.separator + name);
        if (!file.exists()) {
            file = new File(folder, name);
        }
        if (!file.exists()) {
            File[] files = folder.listFiles(pathname -> false);
            BBC.BRUSH_NOT_FOUND.send(player, name);
            return;
        }
        try (DataInputStream in = new DataInputStream(
            new GZIPInputStream(new FileInputStream(file)))) {
            String json = in.readUTF();
            BrushTool tool = BrushTool.fromString(player, session, json);
            BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
            session.setTool(item, tool, player);
            BBC.BRUSH_EQUIPPED.send(player, name);
        } catch (Throwable e) {
            e.printStackTrace();
            BBC.BRUSH_INCOMPATIBLE.send(player);
        }
    }

    @Command(
        name = "/listbrush",
        desc = "List saved brushes",
        descFooter = "List all brushes in the brush directory\n" +
            " -p <page> prints the requested page\n"
    )
    @CommandPermissions("worldedit.brush.list")
    public void list(Actor actor, InjectedValueAccess args,
        @Switch(name = 'p', desc = "Prints the requested page")
            int page) throws WorldEditException {
        String baseCmd = Commands.getAlias(BrushCommands.class, "brush") + " " + Commands.getAlias(BrushOptionsCommands.class, "loadbrush");
        File dir = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
        UtilityCommands.list(dir, actor, args, page, null, true, baseCmd);
//                new RunnableVal2<Message, String[]>() {
//            @Override
//            public void run(Message msg, String[] info) {
//
//            }
//        });
    }

    @Command(
        name = "none",
        aliases = {"/none"},
        desc = "Unbind a bound tool from your current item"
    )
    public void none(Player player, LocalSession session) throws WorldEditException {
        session.setTool(player, null);
        BBC.TOOL_NONE.send(player);
    }

    @Command(
        name = "/",
        aliases = {","},
        desc = "Toggle the super pickaxe function"
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session,
        @Arg(desc = "state", def = "on") String state) throws WorldEditException {
        if (session.hasSuperPickAxe()) {
            if ("on".equals(state)) {
                BBC.SUPERPICKAXE_ENABLED.send(player);
                return;
            }

            session.disableSuperPickAxe();
            BBC.SUPERPICKAXE_DISABLED.send(player);
        } else {
            if ("off".equals(state)) {

                BBC.SUPERPICKAXE_DISABLED.send(player);
                return;
            }
            session.enableSuperPickAxe();
            BBC.SUPERPICKAXE_ENABLED.send(player);
        }
    }

    @Command(
        name = "primary",
        desc = "Set the right click brush",
        descFooter = "Set the right click brush"
    )
    @CommandPermissions("worldedit.brush.primary")
    public void primary(Player player, LocalSession session,
                        @Arg(desc = "The brush command", variable = true) List<String> command) throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(command, " ");
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
                          @Arg(desc = "The brush command", variable = true) List<String> command)
        throws WorldEditException {
        BaseItem item = player.getItemInHand(HandSide.MAIN_HAND);
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item, null, player);
        String cmd = "brush " + StringMan.join(command, " ");
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
    public void visual(Player player, LocalSession session, @Range(min = 0, max = 2) int mode)
        throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
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
        @Arg(name = "mode", desc = "int", def = "0") int mode) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        BBC.BRUSH_TARGET_MODE_SET.send(player, newMode);
    }

    @Command(
        name = "targetmask",
        aliases = {"tarmask", "tm"},
        desc = "Set the targeting mask"
    )
    @CommandPermissions("worldedit.brush.targetmask")
    public void targetMask(Player player, EditSession editSession, LocalSession session, Mask mask) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        tool.setTraceMask(mask);
        BBC.BRUSH_TARGET_MASK_SET.send(player, mask.toString());
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
            BBC.BRUSH_NONE.send(player);
            return;
        }
        tool.setTargetOffset(offset);
        BBC.BRUSH_TARGET_OFFSET_SET.send(player, offset);
    }

    @Command(
        name = "scroll",
        desc = "Toggle between different target modes"
    )
    @CommandPermissions("worldedit.brush.scroll")
    public void scroll(Player player, EditSession editSession, LocalSession session,
    @Switch(name = 'h', desc = "TODO")
        boolean offHand,
    @Arg(desc="Target Modes")
        String modes,
    @Arg(desc = "The scroll action", variable = true)
           List<String> command) throws WorldEditException {
        // TODO NOT IMPLEMENTED Convert ScrollAction to an argument converter
        BrushTool bt = session.getBrushTool(player, false);
        if (bt == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        BrushSettings settings = offHand ? bt.getOffHand() : bt.getContext();
        ScrollAction action = ScrollAction.fromArguments(bt, player, session, StringMan.join(command, " "), true);
        settings.setScrollAction(action);
        if (modes.equalsIgnoreCase("none")) {
            BBC.BRUSH_SCROLL_ACTION_UNSET.send(player);
        } else if (action != null) {
            settings.addSetting(BrushSettings.SettingType.SCROLL_ACTION, modes);
            BBC.BRUSH_SCROLL_ACTION_SET.send(player, modes);
        }
        bt.update();
    }

    @Command(
        name = "mask",
        aliases = {"/mask"},
        desc = "Set the brush destination mask"
    )
    @CommandPermissions({"worldedit.brush.options.mask", "worldedit.mask.brush"})
    public void mask(Player player, LocalSession session, EditSession editSession,
    @Switch(name = 'h', desc = "TODO")
        boolean offHand,
    @Arg(desc = "The destination mask", def = "")
             Mask mask,
             Arguments arguments)
        throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        if (mask == null) {
            BBC.BRUSH_MASK_DISABLED.send(player);
            tool.setMask(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        System.out.println(lastArg + " TODO check this is not the whole command");
        settings.addSetting(BrushSettings.SettingType.MASK, lastArg);
        settings.setMask(mask);
        tool.update();
        BBC.BRUSH_MASK.send(player);
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
              Mask mask,
    @Switch(name = 'h', desc = "TODO")
              boolean offHand,
    Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        if (mask == null) {
            BBC.BRUSH_SOURCE_MASK_DISABLED.send(player);
            tool.setSourceMask(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.SOURCE_MASK, lastArg);
        settings.setSourceMask(mask);
        tool.update();
        BBC.BRUSH_SOURCE_MASK.send(player);
    }

    @Command(
        name = "transform",
        desc = "Set the brush transform"
    )
    @CommandPermissions({"worldedit.brush.options.transform", "worldedit.transform.brush"})
    public void transform(Player player, LocalSession session, EditSession editSession,
    @Arg(desc = "The transform", def = "") ResettableExtent transform,
    @Switch(name = 'h', desc = "TODO")
        boolean offHand,
    Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        if (transform == null) {
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
            tool.setTransform(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.TRANSFORM, lastArg);
        settings.setTransform(transform);
        tool.update();
        BBC.BRUSH_TRANSFORM.send(player);
    }

    @Command(
        name = "mat",
        aliases = {"material"},
        desc = "Set the brush material"
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, EditSession editSession, LocalSession session,
        @Arg(desc = "brush material pattern", def = "") Pattern pattern,
        @Switch(name = 'h', desc = "TODO")
            boolean offHand,
                         Arguments arguments) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        if (pattern == null) {
            BBC.BRUSH_MATERIAL.send(player);
            tool.setFill(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setFill(pattern);
        String lastArg = Iterables.getLast(CommandArgParser.spaceSplit(arguments.get())).getSubstring();
        settings.addSetting(BrushSettings.SettingType.FILL, lastArg);
        tool.update();
        BBC.BRUSH_MATERIAL.send(player);
    }

    @Command(
        name = "range",
        desc = "Set the brush range"
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session,
        @Arg(desc = "Range")
            int range) throws WorldEditException {
        range = Math.max(0, Math.min(256, range));
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        tool.setRange(range);
        BBC.BRUSH_RANGE.send(player);
    }

    @Command(
        name = "size",
        desc = "Set the brush size"
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session,
        @Arg(desc = "The size of the brush", def = "5")
            int radius,
        @Switch(name = 'h', desc = "TODO")
            boolean offHand) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.BRUSH_NONE.f());
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setSize(radius);
        tool.update();
        BBC.BRUSH_SIZE.send(player);
    }


}
