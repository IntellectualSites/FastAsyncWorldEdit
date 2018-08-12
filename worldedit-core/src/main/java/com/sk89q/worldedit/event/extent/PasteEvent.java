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

package com.sk89q.worldedit.event.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.Cancellable;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import java.net.URI;


import static com.sk89q.worldedit.EditSession.Stage;

public class PasteEvent extends Event implements Cancellable {

    private final Player player;
    private final Clipboard clipboard;
    private final URI uri;
    private final Vector to;
    private final Extent extent;
    private boolean cancelled;

    public PasteEvent(Player player, Clipboard clipboard, URI uri, Extent extent, Vector to) {
        this.player = player;
        this.clipboard = clipboard;
        this.uri = uri;
        this.extent = extent;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public URI getURI() {
        return uri;
    }

    public Vector getPosition() {
        return to;
    }

    public Extent getExtent() {
        return extent;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Create a clone of this event with the given stage.
     *
     * @param stage the stage
     * @return a new event
     */
    public PasteEvent clone(Stage stage) {
        PasteEvent clone = new PasteEvent(player, clipboard, uri, extent, to);
        return clone;
    }
}
