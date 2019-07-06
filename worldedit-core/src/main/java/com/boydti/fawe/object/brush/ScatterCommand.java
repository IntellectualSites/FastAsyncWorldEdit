package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.boydti.fawe.wrappers.SilentPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.Location;
import java.util.List;

public class ScatterCommand extends ScatterBrush {
    private final String command;

    public ScatterCommand(int count, int distance, String command) {
        super(count, distance);
        this.command = command;
    }

    @Override
    public void apply(EditSession editSession, LocalBlockVectorSet placed, BlockVector3 position, Pattern p, double size) throws MaxChangedBlocksException {
        int radius = getDistance();
        CuboidRegionSelector selector = new CuboidRegionSelector(editSession.getWorld(), position.subtract(radius, radius, radius), position.add(radius, radius, radius));
        String replaced = command.replace("{x}", position.getBlockX() + "")
                .replace("{y}", Integer.toString(position.getBlockY()))
                .replace("{z}", Integer.toString(position.getBlockZ()))
                .replace("{world}", editSession.getQueue().getWorldName())
                .replace("{size}", Integer.toString(radius));

        FawePlayer fp = editSession.getPlayer();
        Player player = fp.getPlayer();
        fp.setSelection(selector);
        PlayerWrapper wePlayer = new SilentPlayerWrapper(new LocationMaskedPlayerWrapper(player, new Location(player.getExtent(), position.toVector3())));
        List<String> cmds = StringMan.split(replaced, ';');
        for (String cmd : cmds) {
            CommandEvent event = new CommandEvent(wePlayer, cmd);
            PlatformCommandManager.getInstance().handleCommandOnCurrentThread(event);
        }
    }
}
