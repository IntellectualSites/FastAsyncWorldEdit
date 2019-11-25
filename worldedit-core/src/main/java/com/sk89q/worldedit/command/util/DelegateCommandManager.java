package com.sk89q.worldedit.command.util;

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

public class DelegateCommandManager implements CommandManager {
    @Override
    public Command.Builder newCommand(String s) {
        return parent.newCommand(s);
    }

    @Override
    public void register(Command command) {
        parent.register(command);
    }

    @Override
    public void register(String name, Consumer<Command.Builder> registrationProcess) {
        parent.register(name, registrationProcess);
    }

    @Override
    public void registerManager(CommandManager manager) {
        parent.registerManager(manager);
    }

    @Override
    public Stream<Command> getAllCommands() {
        return parent.getAllCommands();
    }

    @Override
    public boolean containsCommand(String name) {
        return parent.containsCommand(name);
    }

    @Override
    public Optional<Command> getCommand(String s) {
        return parent.getCommand(s);
    }

    @Override
    public ImmutableSet<Suggestion> getSuggestions(InjectedValueAccess injectedValueAccess, List<String> list) {
        return parent.getSuggestions(injectedValueAccess, list);
    }

    @Override
    public CommandParseResult parse(InjectedValueAccess injectedValueAccess, List<String> list) {
        return parent.parse(injectedValueAccess, list);
    }

    @Override
    public int execute(InjectedValueAccess context, List<String> args) {
        return parent.execute(context, args);
    }

    @Override
    public <T> void registerConverter(Key<T> key, ArgumentConverter<T> argumentConverter) {
        parent.registerConverter(key, argumentConverter);
    }

    @Override
    public <T> Optional<ArgumentConverter<T>> getConverter(Key<T> key) {
        return parent.getConverter(key);
    }

    private final CommandManager parent;

    public DelegateCommandManager(CommandManager parent) {
        this.parent = parent;
    }
}
