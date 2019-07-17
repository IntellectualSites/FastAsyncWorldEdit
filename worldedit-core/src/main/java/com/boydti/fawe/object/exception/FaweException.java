package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;

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

    private final BBC message;

    public FaweException(BBC reason) {
        this.message = reason;
    }

    @Override
    public String getMessage() {
        return message == null ? null : message.format();
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
