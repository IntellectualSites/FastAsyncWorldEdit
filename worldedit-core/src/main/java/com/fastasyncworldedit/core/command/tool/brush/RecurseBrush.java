package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.function.mask.RadiusMask;
import com.fastasyncworldedit.core.function.visitor.DFSRecursiveVisitor;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

public record RecurseBrush(boolean dfs) implements Brush {

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern to, double size) throws MaxChangedBlocksException {
        Mask mask = editSession.getMask();
        if (mask == null) {
            mask = Masks.alwaysTrue();
        }
        final int radius = (int) size;
        BlockState block = editSession.getBlock(position);
        if (block.getBlockType().getMaterial().isAir()) {
            return;
        }
        final BlockReplace replace = new BlockReplace(editSession, to);
        editSession.setMask(null);
        final int maxY = editSession.getMaxY();
        if (dfs) {
            final Mask radMask = new RadiusMask(0, (int) size);
            DFSRecursiveVisitor visitor = new DFSRecursiveVisitor(mask, replace, Integer.MAX_VALUE, Integer.MAX_VALUE) {
                @Override
                public boolean isVisitable(BlockVector3 from, BlockVector3 to) {
                    int y = to.y();
                    return y < maxY && radMask.test(to) && super.isVisitable(from, to);
                }
            };
            visitor.visit(position);
            Operations.completeBlindly(visitor);
        } else {
            RecursiveVisitor visitor = new RecursiveVisitor(mask, replace, radius, editSession.getMinY(), editSession.getMaxY()) {
                @Override
                public boolean isVisitable(BlockVector3 from, BlockVector3 to) {
                    int y = to.y();
                    return y < maxY && super.isVisitable(from, to);
                }
            };
            visitor.visit(position);
            Operations.completeBlindly(visitor);
        }
    }

}
