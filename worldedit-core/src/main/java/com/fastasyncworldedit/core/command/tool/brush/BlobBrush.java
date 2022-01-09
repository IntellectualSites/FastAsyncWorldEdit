package com.fastasyncworldedit.core.command.tool.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

public record BlobBrush(Vector3 radius, double frequency, double amplitude, double sphericity) implements Brush {

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws
            MaxChangedBlocksException {
        editSession.makeBlob(position, pattern, size, frequency, amplitude, radius, sphericity);
    }

}
