package com.sk89q.worldedit.command.util;

import org.enginehub.piston.Command;
import org.enginehub.piston.inject.InjectedValueAccess;

/**
 * Dummy class
 */
public class CommandQueuedCondition implements Command.Condition {
    private final boolean value;

    public CommandQueuedCondition(boolean value) {
        this.value = value;
    }

    public boolean isQueued() {
        return value;
    }

    @Override
    public boolean satisfied(InjectedValueAccess injectedValueAccess) {
        return true;
    }
}
