package com.boydti.fawe.command;

import com.google.common.collect.ImmutableSet;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandParseResult;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.suggestion.Suggestion;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class CommandProcessor<I, O> implements CommandManager {
    private final CommandManager parent;

    public CommandProcessor(CommandManager parent) {
        this.parent = parent;
    }

    @Override
    public final Command.Builder newCommand(String s) {
        return parent.newCommand(s);
    }

    @Override
    public final void register(Command command) {
        parent.register(command);
    }

    @Override
    public final void register(String name, Consumer<Command.Builder> registrationProcess) {
        parent.register(name, registrationProcess);
    }

    @Override
    public final void registerManager(CommandManager manager) {
        parent.registerManager(manager);
    }

    @Override
    public final Stream<Command> getAllCommands() {
        return parent.getAllCommands();
    }

    @Override
    public final boolean containsCommand(String name) {
        return parent.containsCommand(name);
    }

    @Override
    public final Optional<Command> getCommand(String s) {
        return parent.getCommand(s);
    }

    @Override
    public final ImmutableSet<Suggestion> getSuggestions(InjectedValueAccess injectedValueAccess, List<String> list) {
        return parent.getSuggestions(injectedValueAccess, list);
    }

    @Override
    public final CommandParseResult parse(InjectedValueAccess injectedValueAccess, List<String> list) {
        return parent.parse(injectedValueAccess, list);
    }

    @Override
    public final Object execute(InjectedValueAccess context, List<String> args) {
        args = preprocess(context, args);
        if (args != null) {
            Object result = parent.execute(context, args);
            return process(context, args, result); // TODO NOT IMPLEMENTED (recompile piston)
        } else {
            return null;
        }
    }

    @Override
    public final <T> void registerConverter(Key<T> key, ArgumentConverter<T> argumentConverter) {
        parent.registerConverter(key, argumentConverter);
    }

    @Override
    public final <T> Optional<ArgumentConverter<T>> getConverter(Key<T> key) {
        return parent.getConverter(key);
    }

    public abstract List<String> preprocess(InjectedValueAccess context, List<String> args);

    public abstract Object process(InjectedValueAccess context, List<String> args, Object result);
}
