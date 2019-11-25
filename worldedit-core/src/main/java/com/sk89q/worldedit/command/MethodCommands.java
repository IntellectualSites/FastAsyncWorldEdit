package com.sk89q.worldedit.command;

import com.sk89q.worldedit.command.argument.Arguments;
import java.util.Optional;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

public class MethodCommands {

    public static String getArguments(InjectedValueAccess context) {
        if (context == null) return null;
        Optional<Arguments> arguments = context.injectedValue(Key.of(Arguments.class));
        return arguments.map(Arguments::get).orElse(null);
    }

}
