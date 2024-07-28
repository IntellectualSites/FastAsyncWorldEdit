package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.wrappers.AsyncPlayer;
import com.fastasyncworldedit.core.wrappers.LocationMaskedPlayerWrapper;
import com.fastasyncworldedit.core.wrappers.SilentPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.Location;

import java.util.List;

public class CommandBrush implements Brush {

    private final String command;
    private final boolean print;

    /**
     * New instance
     *
     * @deprecated Use {@link CommandBrush#CommandBrush(String, boolean)}
     */
    @Deprecated(forRemoval = true)
    public CommandBrush(String command) {
        this(command, false);
    }

    /**
     * New instance
     *
     * @param command command to run, or commands split by ';'
     * @param print   if output should be printed to the actor for the run commands
     */
    public CommandBrush(String command, boolean print) {
        this.command = command;
        this.print = print;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        int radius = (int) size;
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
        AsyncPlayer wePlayer = new LocationMaskedPlayerWrapper(
                player,
                new Location(player.getExtent(), position.toVector3())
        );
        if (!print) {
            wePlayer = new SilentPlayerWrapper(wePlayer);
        }
        List<String> cmds = StringMan.split(replaced, ';');
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
