package com.sk89q.worldedit.util.command;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEditException;


import static com.google.common.base.Preconditions.checkNotNull;

public class ProcessedCallable extends DelegateCallable {
    private final CallableProcessor processor;

    public ProcessedCallable(CommandCallable parent, CallableProcessor processor) {
        super(parent);
        checkNotNull(processor);
        this.processor = processor;
    }

    @Override
    public Object call(String arguments, CommandLocals locals, String[] parentCommands) throws CommandException {
        try {
            return processor.process(locals, super.call(arguments, locals, parentCommands));
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }
}
