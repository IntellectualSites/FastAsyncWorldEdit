package com.sk89q.worldedit.internal.command;

import org.enginehub.piston.CommandParameters;
import org.enginehub.piston.gen.CommandCallListener;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.util.ValueProvider;

import java.lang.reflect.Method;
import java.util.Optional;

public class MethodInjector implements CommandCallListener {
    @Override
    public void beforeCall(Method commandMethod, CommandParameters parameters) {
        InjectedValueStore store = parameters.injectedValue(Key.of(InjectedValueStore.class)).get();
        store.injectValue(Key.of(Method.class), ValueProvider.constant(commandMethod));
    }
}
