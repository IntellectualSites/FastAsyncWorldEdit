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

package com.sk89q.worldedit.internal.command;

import com.sk89q.worldedit.command.util.annotation.Confirm;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import java.util.Optional;
import org.enginehub.piston.CommandParameters;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.gen.CommandCallListener;
import org.enginehub.piston.inject.Key;

import java.lang.reflect.Method;

/**
 * Logs called commands to a logger.
 */
public class ConfirmHandler implements CommandCallListener {
    @Override
    public void beforeCall(Method method, CommandParameters parameters) {
        Confirm confirmAnnotation = method.getAnnotation(Confirm.class);
        if (confirmAnnotation == null) {
            return;
        }
        Optional<Actor> actorOpt = parameters.injectedValue(Key.of(Actor.class));

        if (!actorOpt.isPresent()) {
            return;
        }
        Actor actor = actorOpt.get();
        if (!confirmAnnotation.value().passes(actor, parameters, 1)) {
            throw new StopExecutionException(TextComponent.empty());
        }
    }
}
