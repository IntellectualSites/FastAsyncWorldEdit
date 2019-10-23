package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;

public class FaweBlockBagException extends FaweException {
    public FaweBlockBagException() {
        super(BBC.WORLDEDIT_SOME_FAILS_BLOCKBAG);
    }
}