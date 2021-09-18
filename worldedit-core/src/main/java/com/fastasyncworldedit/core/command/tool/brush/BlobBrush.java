package com.fastasyncworldedit.core.command.tool.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

public class BlobBrush implements Brush {

    private final double amplitude;
    private final double frequency;
    private final Vector3 radius;
    private final double sphericity;

    public BlobBrush(Vector3 radius, double frequency, double amplitude, double sphericity) {
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.radius = radius;
        this.sphericity = sphericity;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        editSession.makeBlob(position, pattern, size, frequency, amplitude, radius, sphericity);
    }

}
