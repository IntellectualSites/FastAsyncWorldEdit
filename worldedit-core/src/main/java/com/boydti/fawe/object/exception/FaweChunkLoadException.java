package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;

public class FaweChunkLoadException extends FaweException {
    public FaweChunkLoadException() {
        super(BBC.WORLDEDIT_FAILED_LOAD_CHUNK);
    }
}