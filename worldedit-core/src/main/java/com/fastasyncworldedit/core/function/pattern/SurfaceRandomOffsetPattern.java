package com.fastasyncworldedit.core.function.pattern;

import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.concurrent.ThreadLocalRandom;

public class SurfaceRandomOffsetPattern extends AbstractPattern {

    private final Pattern pattern;
    private final int moves;
    private final int minY;
    private final int maxY;

    private final MutableBlockVector3 cur;
    private final MutableBlockVector3[] buffer;
    private final MutableBlockVector3[] allowed;

    /**
     * Create a new {@link Pattern} instance
     *
     * @param pattern  pattern to apply
     * @param distance number of "spreads" to make
     * @param minY     min applicable y (inclusive
     * @param maxY     max applicable y (inclusive
     */
    public SurfaceRandomOffsetPattern(Pattern pattern, int distance, int minY, int maxY) {
        this.pattern = pattern;
        this.minY = minY;
        this.maxY = maxY;
        this.moves = Math.min(255, distance);
        cur = new MutableBlockVector3();
        this.buffer = new MutableBlockVector3[BreadthFirstSearch.DIAGONAL_DIRECTIONS.length];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new MutableBlockVector3();
        }
        allowed = new MutableBlockVector3[buffer.length];
    }

    @Override
    public boolean apply(final Extent extent, final BlockVector3 get, final BlockVector3 set) throws WorldEditException {
        return super.apply(extent, get, set);
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return pattern.applyBlock(travel(position));
    }

    private BlockVector3 travel(BlockVector3 pos) {
        cur.setComponents(pos);
        MutableBlockVector3 next;
        for (int move = 0; move < moves; move++) {
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                next = buffer[i];
                BlockVector3 dir = BreadthFirstSearch.DIAGONAL_DIRECTIONS[i];
                next.setComponents(
                        cur.getBlockX() + dir.getBlockX(),
                        cur.getBlockY() + dir.getBlockY(),
                        cur.getBlockZ() + dir.getBlockZ()
                );
                if (allowed(next)) {
                    allowed[index++] = next;
                }
            }
            if (index == 0) {
                return cur;
            }
            next = allowed[ThreadLocalRandom.current().nextInt(index)];
            cur.setComponents(next.getBlockX(), next.getBlockY(), next.getBlockZ());
        }
        return cur;
    }

    private boolean allowed(BlockVector3 bv) {
        MutableBlockVector3 v = new MutableBlockVector3(bv);
        BaseBlock block = pattern.applyBlock(bv);
        if (!block.getBlockType().getMaterial().isMovementBlocker()) {
            return false;
        }
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (y < maxY && canPassthrough(v)) {
            v.mutY(y);
            return true;
        }
        v.mutY(y - 1);
        if (y > minY && canPassthrough(v)) {
            v.mutY(y);
            return true;
        }
        v.mutY(y);
        v.mutX(x + 1);
        if (canPassthrough(v)) {
            v.mutX(x);
            return true;
        }
        v.mutX(x - 1);
        if (canPassthrough(v)) {
            v.mutX(x);
            return true;
        }
        v.mutX(x);
        v.mutZ(z + 1);
        if (canPassthrough(v)) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z - 1);
        if (canPassthrough(v)) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z);
        return false;
    }

    private boolean canPassthrough(BlockVector3 v) {
        BaseBlock block = pattern.applyBlock(v);
        return !block.getBlockType().getMaterial().isMovementBlocker();
    }

    @Override
    public BlockVector3 size() {
        return BlockVector3.at(moves, moves, moves);
    }

    @Override
    public Pattern fork() {
        return new SurfaceRandomOffsetPattern(this.pattern.fork(), this.moves, this.minY, this.maxY);
    }

}
