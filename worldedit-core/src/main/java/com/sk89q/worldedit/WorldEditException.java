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

package com.sk89q.worldedit;

/**
 * Parent for all WorldEdit exceptions.
 */
public abstract class WorldEditException extends RuntimeException {

    /**
     * Create a new exception.
     */
    protected WorldEditException() {
    }

    /**
     * Create a new exception with a message.
     *
     * @param message the message
     */
    protected WorldEditException(String message) {
        super(message);
    }

    /**
     * Create a new exception with a message and a cause.
     *
     * @param message the message
     * @param cause the cause
     */
    protected WorldEditException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new exception with a cause.
     *
     * @param cause the cause
     */
    protected WorldEditException(Throwable cause) {
        super(cause);
    }
}
