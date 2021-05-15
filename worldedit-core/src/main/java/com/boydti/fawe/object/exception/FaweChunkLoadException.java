package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.Caption;

public class FaweChunkLoadException extends FaweException {
    public FaweChunkLoadException() {
        super(Caption.of("fawe.cancel.worldedit.failed.load.chunk"));
    }
}
