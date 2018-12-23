package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;

public class LayerBrush implements Brush {

    private final BlockStateHolder[] layers;
    private RecursiveVisitor visitor;
    private MutableBlockVector mutable = new MutableBlockVector();

    public LayerBrush(BlockStateHolder[] layers) {
        this.layers = layers;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern ignore, double size) throws MaxChangedBlocksException {
        final FaweQueue queue = editSession.getQueue();
        final AdjacentAnyMask adjacent = new AdjacentAnyMask(new BlockTypeMask(editSession, BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR));
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);
        visitor = new RecursiveVisitor(vector -> solid.test(vector) && radius.test(vector) && adjacent.test(vector), function -> true);
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
        BlockVectorSet visited = visitor.getVisited();
        BlockStateHolder firstPattern = layers[0];
        visitor = new RecursiveVisitor((Mask) pos -> {
            int depth = visitor.getDepth() + 1;
            if (depth > 1) {
                boolean found = false;
                int previous = layers[depth - 1].getInternalId();
                int previous2 = layers[depth - 2].getInternalId();
                for (BlockVector3 dir : BreadthFirstSearch.DEFAULT_DIRECTIONS) {
                    mutable.setComponents(pos.getBlockX() + dir.getBlockX(), pos.getBlockY() + dir.getBlockY(), pos.getBlockZ() + dir.getBlockZ());
                    if (visitor.isVisited(mutable) && queue.getCachedCombinedId4Data(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous) {
                        mutable.setComponents(pos.getBlockX() + dir.getBlockX() * 2, pos.getBlockY() + dir.getBlockY() * 2, pos.getBlockZ() + dir.getBlockZ() * 2);
                        if (visitor.isVisited(mutable) && queue.getCachedCombinedId4Data(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous2) {
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
        }, pos -> {
            int depth = visitor.getDepth();
            BlockStateHolder currentPattern = layers[depth];
            return editSession.setBlock(pos, currentPattern);
        }, layers.length - 1, editSession);
        for (BlockVector3 pos : visited) {
            visitor.visit(pos);
        }
        Operations.completeBlindly(visitor);
        visitor = null;
    }
}
