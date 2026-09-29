package com.fastasyncworldedit.core.function.visitor;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.math.IntTriple;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Depth-first-search visitor. The visit is performed using a {@link com.fastasyncworldedit.core.math.MutableBlockVector3}
 */
public abstract class DFSVisitor implements Operation {

    private final RegionFunction function;
    private final List<BlockVector3> directions = new ArrayList<>();
    private final Map<Node, AtomicInteger> visited;
    private final ArrayDeque<NodePair> queue;
    private final HashSet<Node> hashQueue;
    private final int maxDepth;
    private final int maxBranch;
    private int affected = 0;

    public DFSVisitor(final RegionFunction function) {
        this(function, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public DFSVisitor(final RegionFunction function, int maxDepth, int maxBranching) {
        this.queue = new ArrayDeque<>();
        this.hashQueue = new LinkedHashSet<>();
        this.visited = new LinkedHashMap<>();
        this.function = function;
        this.directions.add(BlockVector3.UNIT_MINUS_Y);
        this.directions.add(BlockVector3.UNIT_Y);
        this.directions.add(BlockVector3.UNIT_MINUS_X);
        this.directions.add(BlockVector3.UNIT_X);
        this.directions.add(BlockVector3.UNIT_MINUS_Z);
        this.directions.add(BlockVector3.UNIT_Z);
        this.maxDepth = maxDepth;
        this.maxBranch = maxBranching;
    }

    public abstract boolean isVisitable(BlockVector3 from, BlockVector3 to);

    public List<BlockVector3> getDirections() {
        return this.directions;
    }

    private IntTriple[] getIntDirections() {
        IntTriple[] array = new IntTriple[directions.size()];
        for (int i = 0; i < array.length; i++) {
            BlockVector3 dir = directions.get(i);
            array[i] = new IntTriple(dir.x(), dir.y(), dir.z());
        }
        return array;
    }

    public void visit(final BlockVector3 pos) {
        Node node = new Node(pos.x(), pos.y(), pos.z());
        if (!this.hashQueue.contains(node)) {
            isVisitable(pos, pos); // Ignore this, just to initialize mask on this point
            queue.addFirst(new NodePair(null, node, 0));
            hashQueue.add(node);
        }
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        MutableBlockVector3 mutable = new MutableBlockVector3();
        MutableBlockVector3 mutable2 = new MutableBlockVector3();
        IntTriple[] dirs = getIntDirections();

        while (!queue.isEmpty()) {
            NodePair current = queue.poll();
            Node from = current.to;
            hashQueue.remove(from);
            if (visited.containsKey(from)) {
                continue;
            }
            mutable.mutX(from.getX());
            mutable.mutY(from.getY());
            mutable.mutZ(from.getZ());
            function.apply(mutable);
            int countAdd = 0;
            int countAttempt = 0;
            for (IntTriple direction : dirs) {
                mutable2.mutX(from.getX() + direction.x());
                mutable2.mutY(from.getY() + direction.y());
                mutable2.mutZ(from.getZ() + direction.z());
                if (isVisitable(mutable, mutable2)) {
                    Node adjacent = new Node(mutable2.x(), mutable2.y(), mutable2.z());
                    if (!adjacent.equals(current.from)) {
                        AtomicInteger adjacentCount = visited.get(adjacent);
                        if (adjacentCount == null) {
                            if (countAdd++ < maxBranch) {
                                if (!hashQueue.contains(adjacent)) {
                                    if (current.depth == maxDepth) {
                                        countAttempt++;
                                    } else {
                                        hashQueue.add(adjacent);
                                        queue.addFirst(
                                                new NodePair(from, adjacent, current.depth + 1));
                                    }
                                } else {
                                    countAttempt++;
                                }
                            } else {
                                countAttempt++;
                            }
                        } else if (adjacentCount.decrementAndGet() == 0) {
                            visited.remove(adjacent);
                        } else if (hashQueue.contains(adjacent)) {
                            countAttempt++;
                        }
                    }
                }
            }
            if (countAttempt > 0) {
                visited.put(from, new AtomicInteger(countAttempt));
            }
            affected++;
        }
        return null;
    }

    @Override
    public void cancel() {

    }

    public Iterable<Component> getStatusMessages() {
        return Lists.newArrayList(Caption.of(
                "fawe.worldedit.visitor.visitor.block",
                TextComponent.of(getAffected())
        ));
    }

    public int getAffected() {
        return this.affected;
    }

    public static final class Node {

        private int x;
        private int y;
        private int z;

        public Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void set(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        @Override
        public int hashCode() {
            return (x ^ (z << 12)) ^ (y << 24);
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }

        private int getZ() {
            return z;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            Node other = (Node) obj;
            return other.x == x && other.z == z && other.y == y;
        }

    }

    public record NodePair(Node from, Node to, int depth) {

    }

}
