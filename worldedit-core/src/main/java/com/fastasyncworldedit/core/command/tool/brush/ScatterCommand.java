package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.math.LocalBlockVectorSet;
import com.fastasyncworldedit.core.util.StringMan;
import com.fastasyncworldedit.core.wrappers.LocationMaskedPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.formatting.text.Component;

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
        String replaced = command.replace("{x}", position.getBlockX() + "")
                .replace("{y}", Integer.toString(position.getBlockY()))
                .replace("{z}", Integer.toString(position.getBlockZ()))
                .replace("{world}", editSession.getWorld().getName())
                .replace("{size}", Integer.toString(radius));

        Player player = editSession.getPlayer();
        player.setSelection(selector);
        List<String> cmds = StringMan.split(replaced, ';');
        for (String cmd : cmds) {
            Player p = print ?
                    new LocationMaskedPlayerWrapper(player, player.getLocation().setPosition(position.toVector3()), false) :
                    new ScatterCommandPlayerWrapper(player, position);
            CommandEvent event = new CommandEvent(p, cmd, editSession);
            PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        }
    }

    private static final class ScatterCommandPlayerWrapper extends LocationMaskedPlayerWrapper {

        ScatterCommandPlayerWrapper(Player player, BlockVector3 position) {
            super(player, player.getLocation().setPosition(position.toVector3()), false);
        }

        @Override
        public void print(String msg) {
        }

        @Override
        public void print(Component component) {
        }

        @Override
        public void printDebug(String msg) {
        }

        @Override
        public void printError(String msg) {
        }

        @Override
        public void printRaw(String msg) {
        }

    }

}
