package com.fastasyncworldedit.core.internal.exception;

import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.Locale;

public class FaweException extends RuntimeException {

    // DEBUG
    public static final FaweException _enableQueue = new FaweException("enableQueue");
    public static final FaweException _disableQueue = new FaweException("disableQueue");

    private final Component message;
    private final Type type;

    /**
     * New instance. Defaults to {@link FaweException.Type#OTHER}.
     */
    public FaweException(String reason) {
        this(TextComponent.of(reason));
    }

    /**
     * New instance. Defaults to {@link FaweException.Type#OTHER}.
     */
    public FaweException(Component reason) {
        this(reason, Type.OTHER);
    }

    /**
     * New instance of a given {@link FaweException.Type}
     */
    public FaweException(Component reason, Type type) {
        this.message = reason;
        this.type = type;
    }

    @Override
    public String getMessage() {
        return WorldEditText.reduceToText(message, Locale.ROOT);
    }

    public Component getComponent() {
        return message;
    }

    /**
     * Get the {@link FaweException.Type}
     *
     * @return the {@link FaweException.Type}
     */
    public Type getType() {
        return type;
    }

    public static FaweException get(Throwable e) {
        if (e instanceof FaweException) {
            return (FaweException) e;
        }
        Throwable cause = e.getCause();
        if (cause == null) {
            return null;
        }
        return get(cause);
    }

    /**
     * Faster exception throwing/handling if you don't fill the stacktrace.
     *
     * @return a reference to this {@code Throwable} instance.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    public enum Type {
        MANUAL,
        NO_REGION,
        OUTSIDE_REGION,
        MAX_CHECKS,
        MAX_CHANGES,
        LOW_MEMORY,
        MAX_ENTITIES,
        MAX_TILES,
        MAX_ITERATIONS,
        BLOCK_BAG,
        CHUNK,
        PLAYER_ONLY,
        ACTOR_REQUIRED,
        CLIPBOARD,
        HISTORY,
        OTHER
    }

}
