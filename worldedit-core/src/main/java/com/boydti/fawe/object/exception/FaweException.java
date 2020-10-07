package com.boydti.fawe.object.exception;

import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.Locale;

public class FaweException extends RuntimeException {
    // DEBUG
    public static final FaweException _enableQueue = new FaweException("enableQueue");
    public static final FaweException _disableQueue = new FaweException("disableQueue");

    private final Component message;

    public FaweException(String reason) {
        this(TextComponent.of(reason));
    }

    public FaweException(Component reason) {
        this.message = reason;
    }

    @Override
    public String getMessage() {
        return WorldEditText.reduceToText(message, Locale.ROOT);
    }

    public Component getComponent() {
        return message;
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
}
