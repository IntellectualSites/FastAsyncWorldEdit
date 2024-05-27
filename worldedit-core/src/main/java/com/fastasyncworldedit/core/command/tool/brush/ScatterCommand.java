package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.wrappers.AsyncPlayer;
import com.fastasyncworldedit.core.wrappers.LocationMaskedPlayerWrapper;
import com.fastasyncworldedit.core.wrappers.SilentPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.Location;

import java.util.List;

public class ScatterCommand extends ScatterBrush {

    private final String command;
    private final boolean print;

    public ScatterCommand(int count, int distance, String command, boolean print) {
        super(count, distance);
        this.command = command;
        this.print = print;
    }

    @Override
    public void apply(
            EditSession editSession,
            LocalBlockVectorSet placed,
            BlockVector3 position,
            Pattern pattern,
            double size
    ) throws
            MaxChangedBlocksException {
        int radius = getDistance();
        CuboidRegionSelector selector = new CuboidRegionSelector(
                editSession.getWorld(),
                position.subtract(radius, radius, radius),
                position.add(radius, radius, radius)
        );
        String replaced = command.replace("{x}", Integer.toString(position.x()))
                .replace("{y}", Integer.toString(position.y()))
                .replace("{z}", Integer.toString(position.z()))
                .replace("{world}", editSession.getWorld().getName())
                .replace("{size}", Integer.toString(radius));

        Actor actor = editSession.getActor();
        if (!(actor instanceof Player player)) {
            throw FaweCache.PLAYER_ONLY;
        }
        player.setSelection(selector);
        List<String> cmds = StringMan.split(replaced, ';');
        AsyncPlayer wePlayer = new LocationMaskedPlayerWrapper(
                player,
                new Location(player.getExtent(), position.toVector3())
        );
        if (!print) {
            wePlayer = new SilentPlayerWrapper(wePlayer);
        }
        for (String cmd : cmds) {
            if (cmd.isBlank()) {
                continue;
            }
            cmd = cmd.charAt(0) != '/' ? "/" + cmd : cmd;
            cmd = cmd.length() >1 && cmd.charAt(1) == '/' ? cmd.substring(1) : cmd;
            CommandEvent event = new CommandEvent(wePlayer, cmd, editSession);
            PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        }
    }

}
