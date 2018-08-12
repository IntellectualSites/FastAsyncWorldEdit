package com.boydti.fawe.command;

import com.boydti.fawe.config.Commands;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.changeset.CFIChangeSet;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.MethodCommands;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import java.io.IOException;

public class CFICommand extends MethodCommands {

    private final CFICommands child;
    private final SimpleDispatcher dispatcher;

    public CFICommand(WorldEdit worldEdit, ParametricBuilder builder) {
        super(worldEdit);
        this.dispatcher = new SimpleDispatcher();
        this.child = new CFICommands(worldEdit, dispatcher);
        builder.registerMethodsAsCommands(dispatcher, child);
    }

    @Command(
            aliases = {"cfi", "createfromimage"},
            usage = "",
            min = 0,
            max = -1,
            anyFlags = true,
            desc = "Start CreateFromImage"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void cfi(FawePlayer fp, CommandContext context) throws CommandException, IOException {
        CFICommands.CFISettings settings = child.getSettings(fp);
        settings.popMessages(fp);
        dispatch(fp, settings, context);
        HeightMapMCAGenerator gen = settings.getGenerator();
        if (gen != null && gen.isModified()) {
            gen.update();
            CFIChangeSet set = new CFIChangeSet(gen, fp.getUUID());
            LocalSession session = fp.getSession();
            session.remember(fp.getPlayer(), gen, set, fp.getLimit());
        }
    }

    private void dispatch(FawePlayer fp, CFICommands.CFISettings settings, CommandContext context) throws CommandException {
        if (!settings.hasGenerator()) {
            switch (context.argsLength()) {
                case 0: {
                    String hmCmd = child.alias() + " ";
                    if (settings.image == null) {
                        hmCmd += "image";
                    } else {
                        hmCmd = Commands.getAlias(CFICommands.class, "heightmap") + " " + settings.imageArg;
                    }
                    child.msg("What do you want to use as the base?").newline()
                            .text("&7[&aHeightMap&7]").cmdTip(hmCmd).text(" - A heightmap like ").text("&7[&athis&7]").linkTip("http://i.imgur.com/qCd30MR.jpg")
                            .newline()
                            .text("&7[&aEmpty&7]").cmdTip(child.alias() + " empty").text("- An empty map of a specific size")
                            .send(fp);
                    break;
                }
                default: {
                    String remaining = context.getJoinedStrings(0);
                    if (!dispatcher.contains(context.getString(0))) {
                        switch (context.argsLength()) {
                            case 1: {
                                String cmd = Commands.getAlias(CFICommands.class, "heightmap") + " " + context.getJoinedStrings(0);
                                dispatcher.call(cmd, context.getLocals(), new String[0]);
                                return;
                            }
                            case 2: {
                                String cmd = Commands.getAlias(CFICommands.class, "empty") + " " + context.getJoinedStrings(0);
                                dispatcher.call(cmd, context.getLocals(), new String[0]);
                                return;
                            }
                        }
                    }
                    dispatcher.call(remaining, context.getLocals(), new String[0]);
                }
            }
        } else {
            switch (context.argsLength()) {
                case 0:
                    settings.setCategory("");
                    child.mainMenu(fp);
                    break;
                default:
                    dispatcher.call(context.getJoinedStrings(0), context.getLocals(), new String[0]);
                    break;
            }
        }
    }
}
