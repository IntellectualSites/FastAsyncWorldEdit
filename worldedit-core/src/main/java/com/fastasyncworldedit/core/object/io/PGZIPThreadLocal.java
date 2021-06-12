package com.fastasyncworldedit.core.object.io;

public class PGZIPThreadLocal extends ThreadLocal<PGZIPState> {

    private final PGZIPOutputStream parent;

    public PGZIPThreadLocal(PGZIPOutputStream parent) {
        this.parent = parent;
    }

    @Override
    protected PGZIPState initialValue() {
        return new PGZIPState(parent);
    }
}
