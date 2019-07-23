package com.sk89q.worldedit.command;

import com.boydti.fawe.config.Commands;
import com.sk89q.worldedit.command.argument.Arguments;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.inject.InjectedValueAccess;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.WorldEdit;
import org.enginehub.piston.inject.Key;

import java.lang.reflect.Method;
import java.util.Optional;


import static com.google.common.base.Preconditions.checkNotNull;

public class MethodCommands {
    public static Command getCommand() {
        try {
            StackTraceElement[] stack = new Exception().getStackTrace();
            for (StackTraceElement elem : stack) {
                Class<?> clazz = Class.forName(elem.getClassName());
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(elem.getMethodName())) {
                        Command command = method.getAnnotation(Command.class);
                        if (command != null) return Commands.translate(clazz, command);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getArguments(InjectedValueAccess context) {
        if (context == null) return null;
        Optional<Arguments> arguments = context.injectedValue(Key.of(Arguments.class));
        if (arguments.isPresent()) {
            return arguments.get().get();
        }
        return null;
    }

    public static String[] getPermissions(InjectedValueAccess context) {
        CommandPermissions cmdPerms = context.injectedValue(Key.of(CommandPermissions.class)).orElse(null);
        if (cmdPerms != null) {
            return cmdPerms.value();
        }
        try {
            StackTraceElement[] stack = new Exception().getStackTrace();
            for (StackTraceElement elem : stack) {
                Class<?> clazz = Class.forName(elem.getClassName());
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(elem.getMethodName())) {
                        CommandPermissions perm = method.getAnnotation(CommandPermissions.class);
                        if (perm != null) return perm.value();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return new String[0];
    }
}
