package com.boydti.fawe.object.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.IntegerTrio;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DFSVisitor implements Operation {

    private final RegionFunction function;
    private final List<Vector> directions = new ArrayList<>();
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
        this.directions.add(new Vector(0, -1, 0));
        this.directions.add(new Vector(0, 1, 0));
        this.directions.add(new Vector(-1, 0, 0));
        this.directions.add(new Vector(1, 0, 0));
        this.directions.add(new Vector(0, 0, -1));
        this.directions.add(new Vector(0, 0, 1));
        this.maxDepth = maxDepth;
        this.maxBranch = maxBranching;
    }

    public abstract boolean isVisitable(Vector from, Vector to);

    public List<Vector> getDirections() {
        return this.directions;
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
        Node node = new Node(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        if (!this.hashQueue.contains(node)) {
            isVisitable(pos, pos); // Ignore this, just to initialize mask on this point
            queue.addFirst(new NodePair(null, node, 0));
            hashQueue.add(node);
        }
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        NodePair current;
        Node from;
        Node adjacent;
        MutableBlockVector mutable = new MutableBlockVector();
        Vector mutable2 = new Vector();
        int countAdd, countAttempt;
        IntegerTrio[] dirs = getIntDirections();

        for (int layer = 0; !queue.isEmpty(); layer++) {
            current = queue.poll();
            from = current.to;
            hashQueue.remove(from);
            if (visited.containsKey(from)) {
                continue;
            }
            mutable.mutX(from.getX());
            mutable.mutY(from.getY());
            mutable.mutZ(from.getZ());
            function.apply(mutable);
            countAdd = 0;
            countAttempt = 0;
            for (IntegerTrio direction : dirs) {
                mutable2.mutX(from.getX() + direction.x);
                mutable2.mutY(from.getY() + direction.y);
                mutable2.mutZ(from.getZ() + direction.z);
                if (isVisitable(mutable, mutable2)) {
                    adjacent = new Node(mutable2.getBlockX(), mutable2.getBlockY(), mutable2.getBlockZ());
                    if ((current.from == null || !adjacent.equals(current.from))) {
                        AtomicInteger adjacentCount = visited.get(adjacent);
                        if (adjacentCount == null) {
                            if (countAdd++ < maxBranch) {
                                if (!hashQueue.contains(adjacent)) {
                                    if (current.depth == maxDepth) {
                                        countAttempt++;
                                    } else {
                                        hashQueue.add(adjacent);
                                        queue.addFirst(new NodePair(from, adjacent, current.depth + 1));
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

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

    public int getAffected() {
        return this.affected;
    }

    public class NodePair {
        public final Node to;
        public final Node from;
        private final int depth;

        public NodePair(Node from, Node to, int depth) {
            this.from = from;
            this.to = to;
            this.depth = depth;
        }
    }

    public static final class Node {
        private int x, y, z;

        public Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private final void set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private final void set(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.z = node.z;
        }

        @Override
        public final int hashCode() {
            return (x ^ (z << 12)) ^ (y << 24);
        }

        private final int getX() {
            return x;
        }

        private final int getY() {
            return y;
        }

        private final int getZ() {
            return z;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object obj) {
            Node other = (Node) obj;
            return other.x == x && other.z == z && other.y == y;
        }
    }
}
