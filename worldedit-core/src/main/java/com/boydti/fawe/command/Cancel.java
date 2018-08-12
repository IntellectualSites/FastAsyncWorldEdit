package com.boydti.fawe.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.worldedit.EditSession;
import java.util.Collection;
import java.util.UUID;

public class Cancel extends FaweCommand {

    public Cancel() {
        super("fawe.cancel", false);
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (player == null) {
            return false;
        }
        int cancelled = player.cancel(false);
        BBC.WORLDEDIT_CANCEL_COUNT.send(player, cancelled);
        return true;
    }

}
