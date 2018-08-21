package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.boydti.fawe.wrappers.SilentPlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.Location;
import java.util.List;

public class CommandBrush implements Brush {

    private final String command;

    public CommandBrush(String command) {
        this.command = command.charAt(0) == '/' ? "/" + command : command;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        int radius = (int) size;
        CuboidRegionSelector selector = new CuboidRegionSelector(editSession.getWorld(), position.subtract(radius, radius, radius), position.add(radius, radius, radius));
        String replaced = command.replace("{x}", position.getBlockX() + "")
                .replace("{y}", Integer.toString(position.getBlockY()))
                .replace("{z}", Integer.toString(position.getBlockZ()))
                .replace("{world}", editSession.getQueue().getWorldName())
                .replace("{size}", Integer.toString(radius));

        FawePlayer fp = editSession.getPlayer();
        Player player = fp.getPlayer();
        Location face = player.getBlockTraceFace(256, true);
        if (face == null) {
            position = position.add(0, 1, 1);
        } else {
            position = position.add(face.getDirection());
        }
        fp.setSelection(selector);
        PlayerWrapper wePlayer = new SilentPlayerWrapper(new LocationMaskedPlayerWrapper(player, new Location(player.getExtent(), position)));
        List<String> cmds = StringMan.split(replaced, ';');
        for (String cmd : cmds) {
            CommandEvent event = new CommandEvent(wePlayer, cmd);
            CommandManager.getInstance().handleCommandOnCurrentThread(event);
        }
    }
}