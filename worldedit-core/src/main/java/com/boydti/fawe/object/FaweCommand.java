package com.boydti.fawe.object;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;

public abstract class FaweCommand<T> {
    public final String perm;
    public final boolean safe;

    public FaweCommand(String perm) {
        this(perm, true);
    }

    public FaweCommand(final String perm, final boolean safe) {
        this.perm = perm;
        this.safe = safe;
    }

    public String getPerm() {
        return this.perm;
    }

    public boolean executeSafe(final Actor player, final String... args) {
        try {
            if (!safe) {
                execute(player, args);
                return true;
            } else if (player == null) {
                TaskManager.IMP.async(() -> execute(player, args));
            } else {
                if (!player.runAction(() -> execute(player, args), true, true)) {
                    BBC.WORLDEDIT_COMMAND_LIMIT.send(player);
                    return true;
                }
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public abstract boolean execute(final Actor actor, final String... args);
}
