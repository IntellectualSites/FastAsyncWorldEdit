package com.fastasyncworldedit.core.function.mask;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;

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
                mutable.setComponents(
                        pos.x() + dir.x(),
                        pos.y() + dir.y(),
                        pos.z() + dir.z()
                );
                if (visitor.isVisited(mutable) && editSession.getBlock(
                        mutable.x(),
                        mutable.y(),
                        mutable.z()
                ) == previous) {
                    mutable.setComponents(pos.x() + dir.x() * 2, pos.y() + dir.y() * 2,
                            pos.z() + dir.z() * 2
                    );
                    if (visitor.isVisited(mutable)
                            && editSession.getBlock(mutable.x(), mutable.y(), mutable.z()) == previous2) {
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
