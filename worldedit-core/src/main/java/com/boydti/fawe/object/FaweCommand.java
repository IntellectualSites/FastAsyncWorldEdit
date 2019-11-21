package com.boydti.fawe.object;

import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.extension.platform.Actor;

public abstract class FaweCommand<T> {
    public final String perm;
    public final boolean safe;

    public FaweCommand(String perm) {
        this(perm, true);
    }

    public FaweCommand(String perm, boolean safe) {
        this.perm = perm;
        this.safe = safe;
    }

    public String getPerm() {
        return this.perm;
    }

    public boolean executeSafe(Actor player, String... args) {
        try {
            if (!safe) {
                execute(player, args);
                return true;
            } else if (player == null) {
                TaskManager.IMP.async(() -> execute(player, args));
            } else {
                if (!player.runAction(() -> execute(player, args), true, true)) {
                    player.printError(TranslatableComponent.of("fawe.info.worldedit.command.limit"));
                    return true;
                }
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public abstract boolean execute(Actor actor, String... args);
}
