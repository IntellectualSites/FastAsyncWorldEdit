package com.fastasyncworldedit.object.exception;

import com.fastasyncworldedit.configuration.Caption;

public class FaweChunkLoadException extends FaweException {
    public FaweChunkLoadException() {
        super(Caption.of("fawe.cancel.worldedit.failed.load.chunk"));
    }
}
