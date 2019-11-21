package com.boydti.fawe.object.brush.scroll;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.object.clipboard.MultiClipboardHolder;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class Scroll implements ScrollTool {
    private BrushTool tool;

    public enum Action {
        NONE,
        CLIPBOARD,
        MASK,
        PATTERN,
        TARGET_OFFSET,
        RANGE,
        SIZE,
        TARGET
    }

    public static Scroll fromArguments(BrushTool tool, Player player, LocalSession session, String actionArgs, boolean message) {
        String[] split = actionArgs.split(" ");
        Action mode = Action.valueOf(split[0].toUpperCase());
        List<String> args = Arrays.asList(Arrays.copyOfRange(split, 1, split.length));
        return fromArguments(tool, player, session, mode, args, message);
    }

    public static com.boydti.fawe.object.brush.scroll.Scroll fromArguments(BrushTool tool, Player player, LocalSession session, Action mode, List<String> arguments, boolean message) throws InputParseException {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        switch (mode) {
            case NONE:
                return null;
            case CLIPBOARD:
                if (arguments.size() != 2) {
                    if (message) player.print(TranslatableComponent.of("fawe.error.command.syntax" , "clipboard [file]"));
                    return null;
                }
                String filename = arguments.get(1);
                try {
                    MultiClipboardHolder multi = ClipboardFormats.loadAllFromInput(player, filename, null, message);
                    if (multi == null) {
                        return null;
                    }
                    return (new ScrollClipboard(tool, session, multi.getHolders()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            case MASK:
                if (arguments.size() < 2) {
                    if (message) player.print(TranslatableComponent.of("fawe.error.command.syntax" , "mask [mask 1] [mask 2] [mask 3]..."));
                    return null;
                }
                Mask[] masks = new Mask[arguments.size() - 1];
                for (int i = 1; i < arguments.size(); i++) {
                    String arg = arguments.get(i);
                    masks[i - 1] = WorldEdit.getInstance().getMaskFactory().parseFromInput(arg, parserContext);
                }
                return (new ScrollMask(tool, masks));
            case PATTERN:
                if (arguments.size() < 2) {
                    if (message) player.print(TranslatableComponent.of("fawe.error.command.syntax" , "pattern [pattern 1] [pattern 2] [pattern 3]..."));
                    return null;
                }
                Pattern[] patterns = new Pattern[arguments.size() - 1];
                for (int i = 1; i < arguments.size(); i++) {
                    String arg = arguments.get(i);
                    patterns[i - 1] = WorldEdit.getInstance().getPatternFactory().parseFromInput(arg, parserContext);
                }
                return (new ScrollPattern(tool, patterns));
            case TARGET_OFFSET:
                return (new ScrollTargetOffset(tool));
            case RANGE:
                return (new ScrollRange(tool));
            case SIZE:
                return (new ScrollSize(tool));
            case TARGET:
                return (new ScrollTarget(tool));
            default:
                return null;

        }
    }

    public Scroll(BrushTool tool) {
        this.tool = tool;
    }

    public void setTool(BrushTool tool) {
        this.tool = tool;
    }

    public BrushTool getTool() {
        return tool;
    }
}
