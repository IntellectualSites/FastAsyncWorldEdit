package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

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
    public BaseBlock apply(BlockVector3 position) {
        return pattern.apply(travel(position));
    }

    private BlockVector3 travel(BlockVector3 pos) {
        cur.setComponents(pos);
        for (int move = 0; move < moves; move++) {
            int index = 0;
            for (int i = 0; i < allowed.length; i++) {
                next = buffer[i];
                BlockVector3 dir = BreadthFirstSearch.DIAGONAL_DIRECTIONS[i];
                next.setComponents(cur.getBlockX() + dir.getBlockX(), cur.getBlockY() + dir.getBlockY(), cur.getBlockZ() + dir.getBlockZ());
                if (allowed(next.toBlockVector3())) {
                    allowed[index++] = next;
                }
            }
            if (index == 0) {
                return cur.toBlockVector3();
            }
            next = allowed[ThreadLocalRandom.current().nextInt(index)];
            cur.setComponents(next.getBlockX(), next.getBlockY(), next.getBlockZ());
        }
        return cur.toBlockVector3();
    }

    private boolean allowed(BlockVector3 bv) {
    	MutableBlockVector v = new MutableBlockVector(bv);
        BlockStateHolder block = pattern.apply(bv);
        if (!block.getBlockType().getMaterial().isMovementBlocker()) {
            return false;
        }
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        v.mutY(y + 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutY(y);
            return true;
        }
        v.mutY(y - 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutY(y);
            return true;
        }
        v.mutY(y);
        v.mutX(x + 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutX(x);
            return true;
        }
        v.mutX(x - 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutX(x);
            return true;
        }
        v.mutX(x);
        v.mutZ(z + 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z - 1);
        if (canPassthrough(v.toBlockVector3())) {
            v.mutZ(z);
            return true;
        }
        v.mutZ(z);
        return false;
    }

    private boolean canPassthrough(BlockVector3 v) {
        BlockStateHolder block = pattern.apply(v);
        return !block.getBlockType().getMaterial().isMovementBlocker();
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        init();
    }
}
