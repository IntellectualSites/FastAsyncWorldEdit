package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.extent.Extent;

public class FaweException extends RuntimeException {
    public static final FaweChunkLoadException CHUNK = new FaweChunkLoadException();
    public static final FaweBlockBagException BLOCK_BAG = new FaweBlockBagException();
    public static final FaweException MANUAL = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MANUAL);
    public static final FaweException NO_REGION = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_NO_REGION);
    public static final FaweException OUTSIDE_REGION = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_OUTSIDE_REGION);
    public static final FaweException MAX_CHECKS = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
    public static final FaweException MAX_CHANGES = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
    public static final FaweException LOW_MEMORY = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY);
    public static final FaweException MAX_ENTITIES = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_ENTITIES);
    public static final FaweException MAX_TILES = new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_TILES);

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
     * This exception is thrown when a chunk fails to load in time
     * - Chunks are loaded on the main thread to be accessed async
     */
    public static class FaweChunkLoadException extends FaweException {
        public FaweChunkLoadException() {
            super(BBC.WORLDEDIT_FAILED_LOAD_CHUNK);
        }
    }

    public static class FaweBlockBagException extends FaweException {
        public FaweBlockBagException() {
            super(BBC.WORLDEDIT_SOME_FAILS_BLOCKBAG);
        }
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
