package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

public class RandomFullClipboardPattern extends AbstractPattern {
    private final Extent extent;
    private final MutableBlockVector mutable = new MutableBlockVector();
    private final List<ClipboardHolder> clipboards;
    private boolean randomRotate;
    private boolean randomFlip;

    public RandomFullClipboardPattern(Extent extent, List<ClipboardHolder> clipboards, boolean randomRotate, boolean randomFlip) {
        checkNotNull(clipboards);
        this.clipboards = clipboards;
        this.extent = extent;
        this.randomRotate = randomRotate;
    }

    @Override
    public boolean apply(Extent extent, Vector setPosition, Vector getPosition) throws WorldEditException {
        ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
        AffineTransform transform = new AffineTransform();
        if (randomRotate) {
            transform = transform.rotateY(PseudoRandom.random.random(4) * 90);
            holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
        }
        if (randomFlip) {
            transform = transform.scale(new Vector(1, 0, 0).multiply(-2).add(1, 1, 1));
        }
        if (!transform.isIdentity()) {
            holder.setTransform(transform);
        }
        Clipboard clipboard = holder.getClipboard();
        Schematic schematic = new Schematic(clipboard);
        Transform newTransform = holder.getTransform();
        if (newTransform.isIdentity()) {
            schematic.paste(extent, setPosition, false);
        } else {
            schematic.paste(extent, setPosition, false, newTransform);
        }
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
