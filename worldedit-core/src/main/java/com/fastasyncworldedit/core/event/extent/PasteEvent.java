package com.fastasyncworldedit.core.event.extent;

import com.sk89q.worldedit.event.Cancellable;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

import java.net.URI;

import static com.sk89q.worldedit.EditSession.Stage;

public class PasteEvent extends Event implements Cancellable {

    private final Actor actor;
    private final Clipboard clipboard;
    private final URI uri;
    private final BlockVector3 to;
    private final Extent extent;
    private boolean cancelled;

    public PasteEvent(Actor actor, Clipboard clipboard, URI uri, Extent extent, BlockVector3 to) {
        this.actor = actor;
        this.clipboard = clipboard;
        this.uri = uri;
        this.extent = extent;
        this.to = to;
    }

    public Actor getActor() {
        return actor;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public URI getURI() {
        return uri;
    }

    public BlockVector3 getPosition() {
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
        PasteEvent clone = new PasteEvent(actor, clipboard, uri, extent, to);
        return clone;
    }
}
