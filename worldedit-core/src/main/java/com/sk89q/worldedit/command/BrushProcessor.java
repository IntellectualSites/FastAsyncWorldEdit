package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.BrushSettings;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.command.CallableProcessor;

public class BrushProcessor extends MethodCommands implements CallableProcessor<BrushSettings> {
    private final WorldEdit worldEdit;

    public BrushProcessor(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }

    @Override
    public BrushSettings process(CommandLocals locals, BrushSettings settings) throws WorldEditException {
        Actor actor = locals.get(Actor.class);
        LocalSession session = worldEdit.getSessionManager().get(actor);
        session.setTool(null, (Player) actor);
        BrushTool tool = session.getBrushTool((Player) actor);
        if (tool != null) {
            tool.setPrimary(settings);
            tool.setSecondary(settings);
            BBC.BRUSH_EQUIPPED.send(actor, ((String) locals.get("arguments")).split(" ")[1]);
        }
        return null;
    }

    public BrushSettings set(LocalSession session, CommandContext context, Brush brush) throws InvalidToolBindException {
        CommandLocals locals = context.getLocals();
        Actor actor = locals.get(Actor.class);
        BrushSettings bs = new BrushSettings();

        BrushTool tool = session.getBrushTool((Player) actor, false);
        if (tool != null) {
            BrushSettings currentContext = tool.getContext();
            if (currentContext != null) {
                Brush currentBrush = currentContext.getBrush();
                if (currentBrush != null && currentBrush.getClass() == brush.getClass()) {
                    bs = currentContext;
                }
            }
        }

        bs.addPermissions(getPermissions());

        if (locals != null) {
            String args = (String) locals.get("arguments");
            if (args != null) {
                bs.addSetting(BrushSettings.SettingType.BRUSH, args.substring(args.indexOf(' ') + 1));
            }
        }
        return bs.setBrush(brush);
    }
}