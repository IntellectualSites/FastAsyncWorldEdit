package com.fastasyncworldedit.object.exception;

import com.fastasyncworldedit.configuration.Caption;

public class FaweBlockBagException extends FaweException {
    public FaweBlockBagException() {
        super(Caption.of("fawe.error.worldedit.some.fails.blockbag"));
    }
}
