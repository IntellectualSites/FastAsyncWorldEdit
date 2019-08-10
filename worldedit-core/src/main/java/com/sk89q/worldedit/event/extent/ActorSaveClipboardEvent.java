package com.sk89q.worldedit.event.extent;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.Cancellable;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.net.URI;

public class ActorSaveClipboardEvent extends Event implements Cancellable {
    private final Actor actor;
    private final Clipboard clipboard;
    private final URI source, destination;
    private boolean cancelled;

    public ActorSaveClipboardEvent(Actor actor, Clipboard clipboard, URI source, URI destination) {
        this.actor = actor;
        this.clipboard = clipboard;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public URI getSourceURI() {
        return source;
    }

    public URI getDestinationURI() {
        return destination;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public Actor getActor() {
        return actor;
    }
}
