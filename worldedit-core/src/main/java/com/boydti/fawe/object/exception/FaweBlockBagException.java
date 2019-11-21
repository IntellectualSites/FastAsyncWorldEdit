package com.boydti.fawe.object.exception;

import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

public class FaweBlockBagException extends FaweException {
    public FaweBlockBagException() {
        super(TranslatableComponent.of("fawe.error.worldedit.some.fails.blockbag"));
    }
}
