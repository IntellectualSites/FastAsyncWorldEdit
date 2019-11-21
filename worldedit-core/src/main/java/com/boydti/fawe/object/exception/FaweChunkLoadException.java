package com.boydti.fawe.object.exception;

import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

public class FaweChunkLoadException extends FaweException {
    public FaweChunkLoadException() {
        super(TranslatableComponent.of("fawe.cancel.worldedit.failed.load.chunk"));
    }
}
