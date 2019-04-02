/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.util.command.binding;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.util.command.parametric.ArgumentStack;
import com.sk89q.worldedit.util.command.parametric.BindingBehavior;
import com.sk89q.worldedit.util.command.parametric.BindingHelper;
import com.sk89q.worldedit.util.command.parametric.BindingMatch;
import com.sk89q.worldedit.util.command.parametric.ParameterException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.sk89q.worldedit.util.command.parametric.BindingHelper.validate;

/**
 * Standard bindings that should be available to most configurations.
 */
public final class StandardBindings {

    /**
     * Gets a {@link CommandContext} from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return a selection
     */
    @BindingMatch(type = CommandContext.class,
            behavior = BindingBehavior.PROVIDES)
    public CommandContext getCommandContext(ArgumentStack context) {
        context.markConsumed(); // Consume entire stack
        return context.getContext();
    }

    @BindingMatch(
            type = Annotation[].class,
            behavior = BindingBehavior.PROVIDES,
            consumedCount = 0,
            provideModifiers = true,
            provideType = true)
    public Annotation[] getModifiers(ArgumentStack context, Annotation[] modifiers, Type type) throws ParameterException {
        return modifiers;
    }

    @BindingMatch(
            type = Type.class,
            behavior = BindingBehavior.PROVIDES,
            consumedCount = 0,
            provideModifiers = true,
            provideType = true)
    public Type getType(ArgumentStack context, Annotation[] modifiers, Type type) throws ParameterException {
        return type;
    }

    @BindingMatch(
            type = Enum.class,
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true,
            provideType = true)
    public Enum getEnum(ArgumentStack context, Annotation[] modifiers, Type type) throws ParameterException {
        String input = context.next();
        Enum value;
        try {
            value = Enum.valueOf((Class<Enum>) type, input);
        } catch (IllegalArgumentException ignore) {
            try {
                value = Enum.valueOf((Class<Enum>) type, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ParameterException("Invalid input " + input + " for type " + type);
            }
        }
        validate(value.ordinal(), modifiers);
        validate(input, modifiers);
        return value;
    }
    
}
