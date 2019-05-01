package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * A pattern that reads from {@link Clipboard}.
 */
public class ClipboardPattern extends RepeatingExtentPattern {

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public ClipboardPattern(Clipboard clipboard) {
        this(clipboard, BlockVector3.ZERO);
    }

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     * @param offset the offset
     */
    public ClipboardPattern(Clipboard clipboard, BlockVector3 offset) {
        super(clipboard, clipboard.getMinimumPoint(), offset);
    }
}
