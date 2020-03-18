package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import java.util.Arrays;

public class LayerBrush implements Brush {

    private final BlockState[] layers;
    private RecursiveVisitor visitor;
    private MutableBlockVector3 mutable = new MutableBlockVector3();

    public LayerBrush(BlockState[] layers) {
        this.layers = layers;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern ignore, double size) throws MaxChangedBlocksException {
        final AdjacentAnyMask adjacent = new AdjacentAnyMask(new BlockMask(editSession).add(BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR));
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);
        visitor = new RecursiveVisitor(new Mask() {
            @Override
            public boolean test(Extent extent, BlockVector3 vector) {
                return solid.test(extent, vector) && radius.test(extent, vector) && adjacent.test(extent, vector);
            }
        }, function -> true);
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
        BlockVectorSet visited = visitor.getVisited();
        visitor = new RecursiveVisitor(new AbstractExtentMask(editSession) {
            @Override
            public boolean test(Extent extent, BlockVector3 pos) {
                int depth = visitor.getDepth() + 1;
                if (depth > 1) {
                    boolean found = false;
                    BlockState previous = layers[depth - 1];
                    BlockState previous2 = layers[depth - 2];
                    for (BlockVector3 dir : BreadthFirstSearch.DEFAULT_DIRECTIONS) {
                        mutable.setComponents(pos.getBlockX() + dir.getBlockX(), pos.getBlockY() + dir.getBlockY(), pos.getBlockZ() + dir.getBlockZ());
                        if (visitor.isVisited(mutable) && editSession.getBlock(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous) {
                            mutable.setComponents(pos.getBlockX() + dir.getBlockX() * 2, pos.getBlockY() + dir.getBlockY() * 2, pos.getBlockZ() + dir.getBlockZ() * 2);
                            if (visitor.isVisited(mutable) && editSession.getBlock(mutable.getBlockX(), mutable.getBlockY(), mutable.getBlockZ()) == previous2) {
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
                return !adjacent.test(extent, pos);
            }
        }, pos -> {
            int depth = visitor.getDepth();
            BlockState currentPattern = layers[depth];
            return currentPattern.apply(editSession, pos, pos);
        }, layers.length - 1);
        for (BlockVector3 pos : visited) {
            visitor.visit(pos);
        }
        Operations.completeBlindly(visitor);
        visitor = null;
    }
}
