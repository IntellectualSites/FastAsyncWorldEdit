package com.boydti.fawe.command;

import static com.sk89q.worldedit.util.formatting.text.TextComponent.newline;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.object.changeset.CFIChangeSet;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

public class CFICommand extends CommandProcessor<Object, Object> {

    public CFICommand(CommandManager manager) {
        super(manager);
    }

    @Override
    public List<String> preprocess(InjectedValueAccess context, List<String> args) {
        FawePlayer fp = context.injectedValue(Key.of(FawePlayer.class)).orElseThrow(() -> new IllegalStateException("No player"));
        CFICommands.CFISettings settings = CFICommands.getSettings(fp);
        settings.popMessages(fp);
        args = dispatch(fp, settings, args, context);
        HeightMapMCAGenerator gen = settings.getGenerator();
        if (gen != null && gen.isModified()) {
            try {
                gen.update();
                CFIChangeSet set = new CFIChangeSet(gen, fp.getUUID());
                LocalSession session = fp.getSession();
                session.remember(fp.getPlayer(), gen, set, fp.getLimit());
            } catch (IOException e) {
                throw new StopExecutionException(TextComponent.of(e.getMessage()));
            }
        }
        return args;
    }

    @Override
    public Object process(InjectedValueAccess context, List<String> args, Object result) {
        return result;
    }

    private List<String> dispatch(FawePlayer fp, CFICommands.CFISettings settings, List<String> args, InjectedValueAccess context) {
        if (!settings.hasGenerator()) {
            if (args.size() == 0) {
                String hmCmd = "/cfi ";
                if (settings.image == null) {
                    hmCmd += "image";
                } else {
                    hmCmd = "heightmap" + " " + settings.imageArg;
                }
                TextComponent build = TextComponent.builder("What do you want to use as the base?")
                    .append(newline())
                    .append("[HeightMap]")/* TODO .cmdTip(hmCmd).*/.append(" - A heightmap like ")
                    .append("[this]")//TODO .linkTip("http://i.imgur.com/qCd30MR.jpg")
                    .append(newline())
                    .append("[Empty]")//TODO .cmdTip(CFICommands.alias() + " empty")
                    .append("- An empty map of a specific size").build();
                fp.toWorldEditPlayer().print(build);
            } else {
                args = new ArrayList<>(args);
                switch (args.size()) {
                    case 1: {
                        args.add(0, "heightmap");
                        break;
                    }
                    case 2: {
                        args.add(0, "empty");
                        break;
                    }
                }
                return args;
            }
        } else {
            if (args.isEmpty()) {
                settings.setCategory(null);
                CFICommands.mainMenu(fp);
                return null;
            }
        }
        return args;
    }
}
