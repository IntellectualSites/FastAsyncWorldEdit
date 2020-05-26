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

package com.boydti.fawe.object.regions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.polyhedron.Edge;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class PolyhedralRegion extends AbstractRegion {

    /**
     * Vertices that are contained in the convex hull.
     */
    private final Set<BlockVector3> vertices = new LinkedHashSet<>();

    /**
     * Triangles that form the convex hull.
     */
    private final List<Triangle> triangles = new ArrayList<>();

    /**
     * Vertices that are coplanar to the first 3 vertices.
     */
    private final Set<BlockVector3> vertexBacklog = new LinkedHashSet<>();

    /**
     * Minimum point of the axis-aligned bounding box.
     */
    private BlockVector3 minimumPoint;

    /**
     * Maximum point of the axis-aligned bounding box.
     */
    private BlockVector3 maximumPoint;

    /**
     * Accumulator for the barycenter of the polyhedron. Divide by vertices.size() to get the actual center.
     */
    private BlockVector3 centerAccum = BlockVector3.ZERO;

    /**
     * The last triangle that caused a {@link #contains(BlockVector3)} to classify a point as "outside". Used for optimization.
     */
    private Triangle lastTriangle;

    /**
     * Constructs an empty mesh, containing no vertices or triangles.
     *
     * @param world the world
     */
    public PolyhedralRegion(@Nullable World world) {
        super(world);
    }

    /**
     * Constructs an independent copy of the given region.
     *
     * @param region the region to copy
     */
    public PolyhedralRegion(PolyhedralRegion region) {
        this(region.world);
        vertices.addAll(region.vertices);
        triangles.addAll(region.triangles);
        vertexBacklog.addAll(region.vertexBacklog);

        minimumPoint = region.minimumPoint;
        maximumPoint = region.maximumPoint;
        centerAccum = region.centerAccum;
        lastTriangle = region.lastTriangle;
    }

    /**
     * Clears the region, removing all vertices and triangles.
     */
    public void clear() {
        vertices.clear();
        triangles.clear();
        vertexBacklog.clear();

        minimumPoint = null;
        maximumPoint = null;
        centerAccum = BlockVector3.ZERO;
        lastTriangle = null;
    }


    /**
     * Add a vertex to the region.
     *
     * @param vertex the vertex
     * @return true, if something changed.
     */
    public boolean addVertex(BlockVector3 vertex) {
        checkNotNull(vertex);

        lastTriangle = null; // Probably not necessary

        if (vertices.contains(vertex)) {
            return false;
        }

        if (vertices.size() == 3) {
            if (vertexBacklog.contains(vertex)) {
                return false;
            }

            if (containsRaw(vertex)) {
                return vertexBacklog.add(vertex);
            }
        }

        vertices.add(vertex);

        centerAccum = centerAccum.add(vertex);

        if (minimumPoint == null) {
            minimumPoint = maximumPoint = vertex;
        } else {
//            minimumPoint = new MutableBlockVector3(minimumPoint.getMinimum(vertex));
//            maximumPoint = new MutableBlockVector3(maximumPoint.getMaximum(vertex));
            minimumPoint = minimumPoint.getMinimum(vertex);
            maximumPoint = maximumPoint.getMaximum(vertex);
        }

        int size = vertices.size();
        switch (size) {
            case 0:
            case 1:
            case 2:
                // Incomplete, can't make a mesh yet
                return true;

            case 3:
                // Generate minimal mesh to start from
                final BlockVector3[] v = vertices.toArray(new BlockVector3[0]);

                triangles.add((new Triangle(v[0], v[size - 2], v[size - 1])));
                triangles.add((new Triangle(v[0], v[size - 1], v[size - 2])));
                return true;
        }
        final Set<Edge> borderEdges = new LinkedHashSet<>();
        for (Iterator<Triangle> it = triangles.iterator(); it.hasNext(); ) {
            final Triangle triangle = it.next();

            // If the triangle can't be seen, it's not relevant
            if (!triangle.above(vertex)) {
                continue;
            }

            // Remove the triangle from the mesh
            it.remove();

            // ...and remember its edges
            for (int i = 0; i < 3; ++i) {
                final Edge edge = triangle.getEdge(i);
                if (borderEdges.remove(edge)) {
                    continue;
                }

                borderEdges.add(edge);
            }
        }

        // Add triangles between the remembered edges and the new vertex.
        for (Edge edge : borderEdges) {
            com.sk89q.worldedit.regions.polyhedron.Triangle triangle = edge.createTriangle(vertex.toVector3());
            Triangle fTria = new Triangle(triangle.getVertex(0).toBlockPoint(), triangle.getVertex(1).toBlockPoint(), triangle.getVertex(2).toBlockPoint());
            triangles.add(fTria);
        }

        if (!vertexBacklog.isEmpty()) {
            // Remove the new vertex
            vertices.remove(vertex);

            // Clone, clear and work through the backlog
            final List<BlockVector3> vertexBacklog2 = new ArrayList<>(vertexBacklog);
            vertexBacklog.clear();
            for (BlockVector3 vertex2 : vertexBacklog2) {
                addVertex(vertex2);
            }

            // Re-add the new vertex after the backlog.
            vertices.add(vertex);
        }
        return true;
    }

    public boolean isDefined() {
        return !triangles.isEmpty();
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return minimumPoint;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return maximumPoint;
    }

    @Override
    public Vector3 getCenter() {
        return centerAccum.divide(vertices.size()).toVector3();
    }

    @Override
    public void expand(BlockVector3... changes) throws RegionOperationException {
    }

    @Override
    public void contract(BlockVector3... changes) throws RegionOperationException {
    }

    @Override
    public void shift(BlockVector3 change) throws RegionOperationException {
        shiftCollection(vertices, change);
        shiftCollection(vertexBacklog, change);

        for (int i = 0; i < triangles.size(); ++i) {
            final Triangle triangle = triangles.get(i);

            final BlockVector3 v0 = change.add(triangle.getVertex(0));
            final BlockVector3 v1 = change.add(triangle.getVertex(1));
            final BlockVector3 v2 = change.add(triangle.getVertex(2));

            triangles.set(i, new Triangle(v0, v1, v2));
        }

        minimumPoint = change.add(minimumPoint);
        maximumPoint = change.add(maximumPoint);
        centerAccum = change.multiply(vertices.size()).add(centerAccum);
        lastTriangle = null;
    }

    private static void shiftCollection(Collection<BlockVector3> collection, BlockVector3 change) {
        final List<BlockVector3> tmp = new ArrayList<>(collection);
        collection.clear();
        for (BlockVector3 vertex : tmp) {
            collection.add(change.add(vertex));
        }
    }

    @Override
    public boolean contains(BlockVector3 position) {
        if (!isDefined()) {
            return false;
        }
        final int x = position.getBlockX();
        final int y = position.getBlockY();
        final int z = position.getBlockZ();
        final BlockVector3 min = getMinimumPoint();
        final BlockVector3 max = getMaximumPoint();
        if (x < min.getBlockX()) return false;
        if (x > max.getBlockX()) return false;
        if (z < min.getBlockZ()) return false;
        if (z > max.getBlockZ()) return false;
        if (y < min.getBlockY()) return false;
        if (y > max.getBlockY()) return false;
        return containsRaw(position);
    }

    private boolean containsRaw(BlockVector3 pt) {
        if (lastTriangle != null && lastTriangle.contains(pt)) {
            return true;
        }
        for (Triangle triangle : triangles) {
            if (lastTriangle == triangle) {
                continue;
            }
            if (triangle.contains(pt)) {
                lastTriangle = triangle;
                return true;
            }
        }
        return false;
    }

    public Collection<BlockVector3> getVertices() {
        if (vertexBacklog.isEmpty()) {
            return vertices;
        }

        final List<BlockVector3> ret = new ArrayList<>(vertices);
        ret.addAll(vertexBacklog);

        return ret;
    }

    public Collection<Triangle> getTriangles() {
        return triangles;
    }

    @Override
    public AbstractRegion clone() {
        return new PolyhedralRegion(this);
    }
}
