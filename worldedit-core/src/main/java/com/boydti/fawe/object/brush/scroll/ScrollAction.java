package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.IOException;

public abstract class ScrollAction implements ScrollTool {
    private BrushTool tool;

    public static ScrollAction fromArguments(BrushTool tool, Player player, LocalSession session, String arguments, boolean message) throws InputParseException {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        final LocalConfiguration config = WorldEdit.getInstance().getConfiguration();
        String[] split = arguments.split(" ");
        switch (split[0].toLowerCase()) {
            case "none":
                return null;
            case "clipboard":
                if (split.length != 2) {
                    if (message) BBC.COMMAND_SYNTAX.send(player, "clipboard [file]");
                    return null;
                }
                String filename = split[1];
                try {
                    MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player, filename, null, message);
                    if (multi == null) {
                        return null;
                    }
                    return (new ScrollClipboard(tool, session, multi.getHolders()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            case "mask":
                if (split.length < 2) {
                    if (message) BBC.COMMAND_SYNTAX.send(player, "mask [mask 1] [mask 2] [mask 3]...");
                    return null;
                }
                Mask[] masks = new Mask[split.length - 1];
                for (int i = 1; i < split.length; i++) {
                    String arg = split[i];
                    masks[i - 1] = WorldEdit.getInstance().getMaskFactory().parseFromInput(arg, parserContext);
                }
                return (new ScrollMask(tool, masks));
            case "pattern":
                if (split.length < 2) {
                    if (message) BBC.COMMAND_SYNTAX.send(player, "pattern [pattern 1] [pattern 2] [pattern 3]...");
                    return null;
                }
                Pattern[] patterns = new Pattern[split.length - 1];
                for (int i = 1; i < split.length; i++) {
                    String arg = split[i];
                    patterns[i - 1] = WorldEdit.getInstance().getPatternFactory().parseFromInput(arg, parserContext);
                }
                return (new ScrollPattern(tool, patterns));
            case "targetoffset":
                return (new ScrollTargetOffset(tool));
            case "range":
                return (new ScrollRange(tool));
            case "size":
                return (new ScrollSize(tool));
            case "target":
                return (new ScrollTarget(tool));
            default:
                return null;

        }
    }

    public ScrollAction(BrushTool tool) {
        this.tool = tool;
    }

    public void setTool(BrushTool tool) {
        this.tool = tool;
    }

    public BrushTool getTool() {
        return tool;
    }
}
