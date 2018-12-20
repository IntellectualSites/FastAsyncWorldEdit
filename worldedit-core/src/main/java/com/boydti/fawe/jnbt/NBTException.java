package com.boydti.fawe.jnbt;

public class NBTException extends RuntimeException {
    public NBTException(String message) {
        super(message);
    }

    /**
     * Faster exception throwing if you don't fill the stacktrace
     *
     * @return
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
