package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.collection.BlockVectorSet;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector;
import com.sk89q.worldedit.math.Vector3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Performs a breadth-first search starting from points added with
 * {@link #visit(BlockVector3)}. The search continues
 * to a certain adjacent point provided that the method
 * {@link #isVisitable(BlockVector3, BlockVector3)}
 * returns true for that point.
 *
 * <p>As an abstract implementation, this class can be used to implement
 * functionality that starts at certain points and extends outward from
 * those points.</p>
 */
public abstract class BreadthFirstSearch implements Operation {

    public static final BlockVector3[] DEFAULT_DIRECTIONS = new BlockVector3[6];
    public static final BlockVector3[] DIAGONAL_DIRECTIONS;

    static {
        DEFAULT_DIRECTIONS[0] = (new MutableBlockVector(0, -1, 0));
        DEFAULT_DIRECTIONS[1] = (new MutableBlockVector(0, 1, 0));
        DEFAULT_DIRECTIONS[2] = (new MutableBlockVector(-1, 0, 0));
        DEFAULT_DIRECTIONS[3] = (new MutableBlockVector(1, 0, 0));
        DEFAULT_DIRECTIONS[4] = (new MutableBlockVector(0, 0, -1));
        DEFAULT_DIRECTIONS[5] = (new MutableBlockVector(0, 0, 1));
        List<MutableBlockVector> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        MutableBlockVector pos = new MutableBlockVector(x, y, z);
                        if (!list.contains(pos)) {
                            list.add(pos);
                        }
                    }
                }
            }
        }
        Collections.sort(list, new Comparator<BlockVector3>() {
            @Override
            public int compare(BlockVector3 o1, BlockVector3 o2) {
                return (int) Math.signum(o1.lengthSq() - o2.lengthSq());
            }
        });
        DIAGONAL_DIRECTIONS = list.toArray(new BlockVector3[list.size()]);
    }

    private final RegionFunction function;
    private List<BlockVector3> directions = new ArrayList<>();
    private BlockVectorSet visited;
    private final MappedFaweQueue mFaweQueue;
    private BlockVectorSet queue;
    private int currentDepth = 0;
    private final int maxDepth;
    private int affected = 0;
    private int maxBranch = Integer.MAX_VALUE;

    public BreadthFirstSearch(final RegionFunction function) {
        this(function, Integer.MAX_VALUE);
    }

    public BreadthFirstSearch(final RegionFunction function, int maxDepth) {
        this(function, maxDepth, null);
    }

    public BreadthFirstSearch(final RegionFunction function, int maxDepth, HasFaweQueue faweQueue) {
        FaweQueue fq = faweQueue != null ? faweQueue.getQueue() : null;
        this.mFaweQueue = fq instanceof MappedFaweQueue ? (MappedFaweQueue) fq : null;
        this.queue = new BlockVectorSet();
        this.visited = new BlockVectorSet();
        this.function = function;
        this.directions.addAll(Arrays.asList(DEFAULT_DIRECTIONS));
        this.maxDepth = maxDepth;
    }

    public Collection<BlockVector3> getDirections() {
        return this.directions;
    }

    public void setDirections(List<BlockVector3> directions) {
        this.directions = directions;
    }

    private IntegerTrio[] getIntDirections() {
        IntegerTrio[] array = new IntegerTrio[directions.size()];
        for (int i = 0; i < array.length; i++) {
        	BlockVector3 dir = directions.get(i);
            array[i] = new IntegerTrio(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
        }
        return array;
    }

    public void visit(final BlockVector3 pos) {
        if (!isVisited(pos)) {
            isVisitable(pos, pos); // Ignore this, just to initialize mask on this point
            queue.add(pos);
            visited.add(pos);
        }
    }

    public void resetVisited() {
        queue.clear();
        visited.clear();
        affected = 0;
    }

    public void setVisited(BlockVectorSet set) {
        this.visited = set;
    }

    public BlockVectorSet getVisited() {
        return visited;
    }

    public boolean isVisited(BlockVector3 pos) {
        return visited.contains(pos);
    }

    public void setMaxBranch(int maxBranch) {
        this.maxBranch = maxBranch;
    }
    /**
     * Try to visit the given 'to' location.
     *
     * @param from the origin block
     * @param to the block under question
     */
    private void visit(BlockVector3 from, BlockVector3 to) {
        BlockVector3 blockVector = to;
        if (!visited.contains(blockVector)) {
            visited.add(blockVector);
            if (isVisitable(from, to)) {
                queue.add(blockVector);
            }
        }
    }

    /**
     * Return whether the given 'to' block should be visited, starting from the
     * 'from' block.
     *
     * @param from the origin block
     * @param to the block under question
     * @return true if the 'to' block should be visited
     */
    protected abstract boolean isVisitable(BlockVector3 from, BlockVector3 to);

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return affected;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        MutableBlockVector mutable = new MutableBlockVector();
        MutableBlockVector mutable2 = new MutableBlockVector();
        boolean shouldTrim = false;
        IntegerTrio[] dirs = getIntDirections();
        BlockVectorSet tempQueue = new BlockVectorSet();
        BlockVectorSet chunkLoadSet = new BlockVectorSet();
        for (currentDepth = 0; !queue.isEmpty() && currentDepth <= maxDepth; currentDepth++) {
            if (mFaweQueue != null && Settings.IMP.QUEUE.PRELOAD_CHUNKS > 1) {
                int cx = Integer.MIN_VALUE;
                int cz = Integer.MIN_VALUE;
                for (BlockVector3 from : queue) {
                    for (IntegerTrio direction : dirs) {
                        int x = from.getBlockX() + direction.x;
                        int z = from.getBlockZ() + direction.z;
                        if (cx != (cx = x >> 4) || cz != (cz = z >> 4)) {
                            int y = from.getBlockY() + direction.y;
                            if (y < 0 || y >= 256) {
                                continue;
                            }
                            if (!visited.contains(x, y, z)) {
                                chunkLoadSet.add(cx, 0, cz);
                            }
                        }
                    }
                }
                for (BlockVector3 chunk : chunkLoadSet) {
                    mFaweQueue.queueChunkLoad(chunk.getBlockX(), chunk.getBlockZ());
                }
            }
            for (BlockVector3 from : queue) {
                if (function.apply(from)) affected++;
                for (int i = 0, j = 0; i < dirs.length && j < maxBranch; i++) {
                    IntegerTrio direction = dirs[i];
                    int y = from.getBlockY() + direction.y;
                    if (y < 0 || y >= 256) {
                        continue;
                    }
                    int x = from.getBlockX() + direction.x;
                    int z = from.getBlockZ() + direction.z;
                    if (!visited.contains(x, y, z)) {
                        mutable2.mutX(x);
                        mutable2.mutY(y);
                        mutable2.mutZ(z);
                        if (isVisitable(from, mutable2)) {
                            j++;
                            visited.add(x, y, z);
                            tempQueue.add(x, y, z);
                        }
                    }
                }
//=======
//        BlockVector3 position;
//        
//        while ((position = queue.poll()) != null) {
//            if (function.apply(position)) {
//                affected++;
//            }
//
//            for (BlockVector3 dir : directions) {
//                visit(position, position.add(dir));
//>>>>>>> 399e0ad5... Refactor vector system to be cleaner
            }
            if (currentDepth == maxDepth) {
                break;
            }
            int size = queue.size();
            BlockVectorSet tmp = queue;
            queue = tempQueue;
            tmp.clear();
            chunkLoadSet.clear();
            tempQueue = tmp;

        }
        return null;
    }

    public int getDepth() {
        return currentDepth;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

    @Override
    public void cancel() {
    }


}
