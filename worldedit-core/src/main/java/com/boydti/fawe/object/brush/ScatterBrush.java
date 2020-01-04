package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.collection.BlockVectorSet;
import com.boydti.fawe.object.collection.LocalBlockVectorSet;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.boydti.fawe.object.mask.SurfaceMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.DelegateExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ScatterBrush implements Brush {

    private final int count;
    private final int distance;
    private Mask mask;
    private AdjacentAnyMask surface;

    public ScatterBrush(int count, int distance) {
        this.count = count;
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        this.mask = editSession.getMask();
        if (this.mask == null) {
            this.mask = Masks.alwaysTrue();
        }
        surface = new SurfaceMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);

        final int distance = Math.min((int) size, this.distance);

        RecursiveVisitor visitor = new RecursiveVisitor(new MaskUnion(radius, surface).withExtent(editSession), function -> true);
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
        BlockVectorSet visited = visitor.getVisited();
        int length = visited.size();
        if (size == 0) {
            length = 1;
            visited.add(position);
        }
        LocalBlockVectorSet placed = new LocalBlockVectorSet();
        int maxFails = 1000;
        for (int i = 0; i < count; i++) {
            int index = ThreadLocalRandom.current().nextInt(length);
            BlockVector3 pos = visited.get(index);
            if (pos != null && canApply(editSession, pos)) {
                int x = pos.getBlockX();
                int y = pos.getBlockY();
                int z = pos.getBlockZ();
                if (placed.containsRadius(x, y, z, distance)) {
                    if (maxFails-- <= 0) {
                        break;
                    }
                    i--;
                    continue;
                }
                placed.add(x, y, z);
                apply(editSession, placed, pos, pattern, size);
            }
        }
        finish(editSession, placed, position, pattern, size);
    }

    public void finish(EditSession editSession, LocalBlockVectorSet placed, BlockVector3 pos, Pattern pattern, double size) {
    }

    public boolean canApply(EditSession editSession, BlockVector3 pos) {
        return mask.test(editSession, pos);
    }

    public BlockVector3 getDirection(Extent extent, BlockVector3 pt) {
        return surface.direction(extent, pt);
    }

    public void apply(EditSession editSession, LocalBlockVectorSet placed, BlockVector3 pt, Pattern p, double size) throws MaxChangedBlocksException {
        editSession.setBlock(pt, p);
    }
}
