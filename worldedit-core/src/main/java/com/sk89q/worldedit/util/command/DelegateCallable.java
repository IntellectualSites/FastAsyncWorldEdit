package com.sk89q.worldedit.util.command;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import java.util.List;

public class DelegateCallable implements CommandCallable {
    private final CommandCallable parent;

    public CommandCallable getParent() {
        return parent;
    }

    public DelegateCallable(CommandCallable parent) {
        this.parent = parent;
    }

    @Override
    public Object call(String arguments, CommandLocals locals, String[] parentCommands) throws CommandException {
        return parent.call(arguments, locals, parentCommands);
    }

    @Override
    public Description getDescription() {
        return parent.getDescription();
    }

    @Override
    public boolean testPermission(CommandLocals locals) {
        return parent.testPermission(locals);
    }

    @Override
    public List<String> getSuggestions(String arguments, CommandLocals locals) throws CommandException {
        return parent.getSuggestions(arguments, locals);
    }
}
