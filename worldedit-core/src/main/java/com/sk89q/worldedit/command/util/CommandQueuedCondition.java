package com.sk89q.worldedit.command.util;

import org.enginehub.piston.Command;
import org.enginehub.piston.inject.InjectedValueAccess;

/**
 * Dummy class
 */
public class CommandQueuedCondition implements Command.Condition {
    private final boolean isQueued;

    public CommandQueuedCondition(boolean isQueued) {
        this.isQueued = isQueued;
    }

    public boolean isQueued() {
        return isQueued;
    }

    @Override
    public boolean satisfied(InjectedValueAccess injectedValueAccess) {
        return true;
    }
}
