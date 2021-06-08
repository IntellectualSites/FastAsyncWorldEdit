package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.Caption;

public class FaweBlockBagException extends FaweException {
    public FaweBlockBagException() {
        super(Caption.of("fawe.error.worldedit.some.fails.blockbag"));
    }
}
