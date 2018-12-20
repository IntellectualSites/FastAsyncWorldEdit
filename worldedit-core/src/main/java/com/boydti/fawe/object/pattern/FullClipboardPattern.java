package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import java.io.IOException;
import java.io.NotSerializableException;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that reads from {@link Clipboard}.
 */
public class FullClipboardPattern extends AbstractExtentPattern {
    private final Clipboard clipboard;
    private final MutableBlockVector mutable = new MutableBlockVector();

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public FullClipboardPattern(Extent extent, Clipboard clipboard) {
        super(extent);
        checkNotNull(clipboard);
        this.clipboard = clipboard;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        Region region = clipboard.getRegion();
        ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), extent, setPosition);
        copy.setSourceMask(new ExistingBlockMask(clipboard));
        Operations.completeBlindly(copy);
        return true;
    }

    @Override
    public BaseBlock apply(Vector position) {
        throw new IllegalStateException("Incorrect use. This pattern can only be applied to an extent!");
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new NotSerializableException("Clipboard cannot be serialized!");
    }
}