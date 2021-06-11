package com.boydti.fawe.object.brush.mask;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Arrays;

public class LayerBrushMask extends AbstractExtentMask {

    private final EditSession editSession;
    private final RecursiveVisitor visitor;
    private final BlockState[] layers;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private final Mask adjacent;

    public LayerBrushMask(EditSession editSession, RecursiveVisitor visitor, BlockState[] layers, Mask adjacent) {
        super(editSession);
        this.editSession = editSession;
        this.visitor = visitor;
        this.layers = layers;
        this.adjacent = adjacent;
    }

    @Override
    public boolean test(Extent extent, BlockVector3 vector) {
        return test(vector);
    }

    @Override
    public boolean test(BlockVector3 pos) {
        int depth = (visitor.getDepth() + 1) % layers.length;
        if (depth > 1) {
            boolean found = false;
            BlockState previous = layers[depth - 1];
            BlockState previous2 = layers[depth - 2];
            for (BlockVector3 dir : BreadthFirstSearch.DEFAULT_DIRECTIONS) {
                mutable.setComponents(pos.getBlockX() + dir.getBlockX(), pos.getBlockY() + dir.getBlockY(), pos.getBlockZ() + dir.getBlockZ());
                if (visitor.isVisited(mutable) && editSession.getBlock(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous) {
                    mutable.setComponents(pos.getBlockX() + dir.getBlockX() * 2, pos.getBlockY() + dir.getBlockY() * 2,
                        pos.getBlockZ() + dir.getBlockZ() * 2);
                    if (visitor.isVisited(mutable)
                        && editSession.getBlock(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous2) {
                        found = true;
                        break;
                    } else {
                        return false;
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return !adjacent.test(pos);
    }

    @Override
    public Mask copy() {
        return new LayerBrushMask(editSession, visitor, layers.clone(), adjacent.copy());
    }
}
