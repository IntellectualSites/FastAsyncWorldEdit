package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.extent.Extent;

public class FaweException extends RuntimeException {
    // DEBUG
    public static final FaweException _enableQueue;
    public static final FaweException _disableQueue;

    static {
        try {
            _enableQueue = new FaweException(Extent.class.getDeclaredMethod("enableQueue").toString());
            _disableQueue = new FaweException(Extent.class.getDeclaredMethod("disableQueue").toString());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private final String message;

    public FaweException(String reason) {
        this.message = reason;
    }

    public FaweException(BBC reason) {
        this(reason.format());
    }

    @Override
    public String getMessage() {
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
     * Faster exception throwing/handling if you don't fill the stacktrace
     *
     * @return
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
