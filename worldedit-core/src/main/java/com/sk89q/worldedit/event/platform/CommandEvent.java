/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.event.platform;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.AbstractCancellable;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is currently only for internal use. Do not post or catch this event.
 */
public class CommandEvent extends AbstractCancellable {

    private final Actor actor;
    private final String arguments;
    //FAWE start
    @Nullable
    private final EditSession session;
    //FAWE end

    /**
     * Create a new instance.
     *
     * @param actor     the player
     * @param arguments the arguments
     */
    public CommandEvent(Actor actor, String arguments) {
        checkNotNull(actor);
        checkNotNull(arguments);

        this.actor = actor;
        this.arguments = arguments;
        //FAWE start
        this.session = null;
    }

    /**
     * Create a new instance.
     *
     * @param actor       the player
     * @param arguments   the arguments
     * @param editSession the editsession
     */
    public CommandEvent(Actor actor, String arguments, @Nullable EditSession editSession) {
        checkNotNull(actor);
        checkNotNull(arguments);

        this.actor = actor;
        this.arguments = arguments;
        this.session = editSession;
    }

    @Nullable
    public EditSession getSession() {
        return session;
    }
    //FAWE end

    /**
     * Get the actor that issued the command.
     *
     * @return the actor that issued the command
     */
    public Actor getActor() {
        return actor;
    }

    /**
     * Get the arguments.
     *
     * @return the arguments
     */
    public String getArguments() {
        return arguments;
    }

    //FAWE start
    @Override
    public boolean call() {
        PlatformCommandManager.getInstance().handleCommandOnCurrentThread(this);
        return true;
    }
    //FAWE end
}
