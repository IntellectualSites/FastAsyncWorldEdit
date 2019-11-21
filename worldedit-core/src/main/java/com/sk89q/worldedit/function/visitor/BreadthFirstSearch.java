/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.boydti.fawe.object.collection.BlockVectorSet;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
        DEFAULT_DIRECTIONS[0] = (BlockVector3.at(0, -1, 0));
        DEFAULT_DIRECTIONS[1] = (BlockVector3.at(0, 1, 0));
        DEFAULT_DIRECTIONS[2] = (BlockVector3.at(-1, 0, 0));
        DEFAULT_DIRECTIONS[3] = (BlockVector3.at(1, 0, 0));
        DEFAULT_DIRECTIONS[4] = (BlockVector3.at(0, 0, -1));
        DEFAULT_DIRECTIONS[5] = (BlockVector3.at(0, 0, 1));
        List<BlockVector3> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        BlockVector3 pos = BlockVector3.at(x, y, z);
                        if (!list.contains(pos)) {
                            list.add(pos);
                        }
                    }
                }
            }
        }
        list.sort((o1, o2) -> (int) Math.signum(o1.lengthSq() - o2.lengthSq()));
        DIAGONAL_DIRECTIONS = list.toArray(new BlockVector3[0]);
    }

    private final RegionFunction function;
    private BlockVectorSet queue = new BlockVectorSet();
    private BlockVectorSet visited = new BlockVectorSet();
    private BlockVector3[] directions;
    private int affected = 0;
    private int currentDepth = 0;
    private final int maxDepth;
    private int maxBranch = Integer.MAX_VALUE;

    /**
     * Create a new instance.
     *
     * @param function the function to apply to visited blocks
     */
    public BreadthFirstSearch(RegionFunction function) {
        this(function, Integer.MAX_VALUE);
        checkNotNull(function);
    }

    public BreadthFirstSearch(RegionFunction function, int maxDepth) {
        checkNotNull(function);
        this.function = function;
        this.directions = DEFAULT_DIRECTIONS;
        this.maxDepth = maxDepth;
    }

    public void setDirections(BlockVector3... directions) {
        this.directions = directions;
    }

    public void setDirections(Collection<BlockVector3> directions) {
        setDirections(directions.toArray(new BlockVector3[0]));
    }

    /**
     * Get the list of directions will be visited.
     *
     * <p>Directions are {@link BlockVector3}s that determine
     * what adjacent points area available. Vectors should not be
     * unit vectors. An example of a valid direction is
     * {@code BlockVector3.at(1, 0, 1)}.</p>
     *
     * <p>The list of directions can be cleared.</p>
     *
     * @return the list of directions
     */
    public Collection<BlockVector3> getDirections() {
        return Arrays.asList(directions);
    }

    /**
     * Add the directions along the axes as directions to visit.
     */
    public void addAxes() {
        HashSet<BlockVector3> set = Sets.newHashSet(directions);
        set.add(BlockVector3.UNIT_MINUS_Y);
        set.add(BlockVector3.UNIT_Y);
        set.add(BlockVector3.UNIT_MINUS_X);
        set.add(BlockVector3.UNIT_X);
        set.add(BlockVector3.UNIT_MINUS_Z);
        set.add(BlockVector3.UNIT_Z);
        setDirections(set);
    }

    /**
     * Add the diagonal directions as directions to visit.
     */
    public void addDiagonal() {
        HashSet<BlockVector3> set = Sets.newHashSet(directions);
        set.add(Direction.NORTHEAST.toBlockVector());
        set.add(Direction.SOUTHEAST.toBlockVector());
        set.add(Direction.SOUTHWEST.toBlockVector());
        set.add(Direction.NORTHWEST.toBlockVector());
        setDirections(set);
    }

    /**
     * Add the given location to the list of locations to visit, provided
     * that it has not been visited. The position passed to this method
     * will still be visited even if it fails
     * {@link #isVisitable(BlockVector3, BlockVector3)}.
     *
     * <p>This method should be used before the search begins, because if
     * the position <em>does</em> fail the test, and the search has already
     * visited it (because it is connected to another root point),
     * the search will mark the position as "visited" and a call to this
     * method will do nothing.</p>
     *
     * @param position the position
     */
    public void visit(BlockVector3 position) {
        if (!visited.contains(position)) {
            queue.add(position);
            visited.add(position);
        }
    }

    /**
     * Try to visit the given 'to' location.
     *
     * @param from the origin block
     * @param to the block under question
     */
    private void visit(BlockVector3 from, BlockVector3 to) {
        if (!visited.contains(to)) {
            visited.add(to);
            if (isVisitable(from, to)) {
                queue.add(to);
            }
        }
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
        MutableBlockVector3 mutable = new MutableBlockVector3();
        BlockVector3[] dirs = directions;
        BlockVectorSet tempQueue = new BlockVectorSet();
        for (currentDepth = 0; !queue.isEmpty() && currentDepth <= maxDepth; currentDepth++) {
            for (BlockVector3 from : queue) {
                if (function.apply(from)) affected++;
                for (int i = 0, j = 0; i < dirs.length && j < maxBranch; i++) {
                    BlockVector3 direction = dirs[i];
                    int y = from.getBlockY() + direction.getY();
                    if (y < 0 || y >= 256) {
                        continue;
                    }
                    int x = from.getBlockX() + direction.getX();
                    int z = from.getBlockZ() + direction.getZ();
                    if (!visited.contains(x, y, z)) {
                        if (isVisitable(from, mutable.setComponents(x, y, z))) {
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
            BlockVectorSet tmp = queue;
            queue = tempQueue;
            tmp.clear();
            tempQueue = tmp;
        }

        return null;
    }

    public int getDepth() {
        return currentDepth;
    }

    @Override
    public void cancel() {
        queue.clear();
        visited.clear();
        affected = 0;
    }

    @Override
    public Iterable<Component> getStatusMessages() {
        return Lists.newArrayList(TranslatableComponent.of(
                "worldedit.operation.affected.block",
                TextComponent.of(getAffected())
        ).color(TextColor.GRAY));
    }

}
