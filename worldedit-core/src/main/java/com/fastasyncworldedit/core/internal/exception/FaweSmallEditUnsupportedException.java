package com.fastasyncworldedit.core.internal.exception;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

public class FaweSmallEditUnsupportedException extends FaweException {

    private static final Component message = TextComponent.of(
            "y cannot be outside range 0-255 for small-edits=true. History will NOT work on edits outside this range.");

    public FaweSmallEditUnsupportedException() {
        super(message, Type.HISTORY);
    }

}
