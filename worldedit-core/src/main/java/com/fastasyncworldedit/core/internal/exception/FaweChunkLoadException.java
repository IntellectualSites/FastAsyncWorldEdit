package com.fastasyncworldedit.core.internal.exception;

import com.fastasyncworldedit.core.configuration.Caption;

public class FaweChunkLoadException extends FaweException {

    public FaweChunkLoadException() {
        super(Caption.of("fawe.cancel.worldedit.failed.load.chunk"));
    }

}
