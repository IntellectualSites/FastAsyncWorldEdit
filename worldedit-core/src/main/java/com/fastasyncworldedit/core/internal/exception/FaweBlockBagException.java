package com.fastasyncworldedit.core.internal.exception;

import com.fastasyncworldedit.core.configuration.Caption;

public class FaweBlockBagException extends FaweException {
    public FaweBlockBagException() {
        super(Caption.of("fawe.error.worldedit.some.fails.blockbag"));
    }
}
