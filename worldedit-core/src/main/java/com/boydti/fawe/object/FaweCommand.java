package com.boydti.fawe.object;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;

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

    public boolean executeSafe(final FawePlayer<T> player, final String... args) {
        try {
            if (!safe) {
                execute(player, args);
                return true;
            } else if (player == null) {
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        execute(player, args);
                    }
                });
            } else {
                if (!player.runAction(new Runnable() {
                    @Override
                    public void run() {
                        execute(player, args);
                    }
                }, true, true)) {
                    BBC.WORLDEDIT_COMMAND_LIMIT.send(player);
                    return true;
                }
            }
            return true;
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return false;
    }

    public abstract boolean execute(final FawePlayer<T> player, final String... args);
}
