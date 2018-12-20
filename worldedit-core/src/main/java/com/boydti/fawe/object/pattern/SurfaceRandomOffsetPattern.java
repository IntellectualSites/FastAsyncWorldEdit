package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;

public class SurfaceRandomOffsetPattern extends AbstractPattern {
    private final Pattern pattern;
    private int moves;

    private transient MutableBlockVector cur;
    private transient MutableBlockVector[] buffer;
    private transient MutableBlockVector[] allowed;
    private transient MutableBlockVector next;

    public SurfaceRandomOffsetPattern(Pattern pattern, int distance) {
        this.pattern = pattern;
        this.moves = Math.min(255, distance);
        init();
    }

    private void init() {
        cur = new MutableBlockVector();
        this.buffer = new MutableBlockVector[BreadthFirstSearch.DIAGONAL_DIRECTIONS.length];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new MutableBlockVector();
        }
        allowed = new MutableBlockVector[buffer.length];
    }

    @Override
    public BlockStateHolder apply(Vector position) {
        return pattern.apply(travel(position));
    }

    private Vector travel(Vector pos) {
        cur.setComponents(pos);
        for (int move = 0; move < moves; move++) {
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                next = buffer[i];
                Vector dir = BreadthFirstSearch.DIAGONAL_DIRECTIONS[i];
                next.setComponents(cur.getBlockX() + dir.getBlockX(), cur.getBlockY() + dir.getBlockY(), cur.getBlockZ() + dir.getBlockZ());
                if (allowed(next)) {
                    allowed[index++] = next;
                }
            }
            if (index == 0) {
                return cur;
            }
            next = allowed[PseudoRandom.random.nextInt(index)];
            cur.setComponents(next.getBlockX(), next.getBlockY(), next.getBlockZ());
        }
        return cur;
    }

    private boolean allowed(Vector v) {
        BlockStateHolder block = pattern.apply(v);
        if (!block.getBlockType().getMaterial().isMovementBlocker()) {
            return false;
        }
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (canPassthrough(v)) {
            v.mutY(y);
            return true;
        }
        v.mutY(y - 1);
        if (canPassthrough(v)) {
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

    private boolean canPassthrough(Vector v) {
        BlockStateHolder block = pattern.apply(v);
        return !block.getBlockType().getMaterial().isMovementBlocker();
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }
}