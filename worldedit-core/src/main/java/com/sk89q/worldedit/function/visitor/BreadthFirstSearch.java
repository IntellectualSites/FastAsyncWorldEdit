package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class BreadthFirstSearch implements Operation {

    public static final Vector[] DEFAULT_DIRECTIONS = new Vector[6];
    public static final Vector[] DIAGONAL_DIRECTIONS;

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
        Collections.sort(list, new Comparator<Vector>() {
            @Override
            public int compare(Vector o1, Vector o2) {
                return (int) Math.signum(o1.lengthSq() - o2.lengthSq());
            }
        });
        DIAGONAL_DIRECTIONS = list.toArray(new Vector[list.size()]);
    }

    private final RegionFunction function;
    private List<Vector> directions = new ArrayList<>();
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

    public abstract boolean isVisitable(Vector from, Vector to);

    public Collection<Vector> getDirections() {
        return this.directions;
    }

    public void setDirections(List<Vector> directions) {
        this.directions = directions;
    }

    private IntegerTrio[] getIntDirections() {
        IntegerTrio[] array = new IntegerTrio[directions.size()];
        for (int i = 0; i < array.length; i++) {
            Vector dir = directions.get(i);
            array[i] = new IntegerTrio(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
        }
        return array;
    }

    public void visit(final Vector pos) {
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

    public boolean isVisited(Vector pos) {
        return visited.contains(pos);
    }

    public void setMaxBranch(int maxBranch) {
        this.maxBranch = maxBranch;
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
                for (Vector from : queue) {
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
                for (Vector chunk : chunkLoadSet) {
                    mFaweQueue.queueChunkLoad(chunk.getBlockX(), chunk.getBlockZ());
                }
            }
            for (Vector from : queue) {
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

    public int getAffected() {
        return this.affected;
    }

    @Override
    public void cancel() {
    }

    public static Class<?> inject() {
        return BreadthFirstSearch.class;
    }
}
