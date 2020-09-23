package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.brush.mask.LayerBrushMask;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Arrays;

public class LayerBrush implements Brush {

    private final BlockState[] layers;
    private RecursiveVisitor visitor;

    public LayerBrush(BlockState[] layers) {
        this.layers = layers;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern ignore, double size) throws MaxChangedBlocksException {
        final AdjacentAnyMask adjacent = new AdjacentAnyMask(new BlockMask(editSession).add(BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR));
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);
        visitor = new RecursiveVisitor(new MaskIntersection(adjacent, solid, radius), function -> true);
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
        BlockVectorSet visited = visitor.getVisited();
        visitor = new RecursiveVisitor(new LayerBrushMask(editSession, visitor, layers, adjacent), pos -> {
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
