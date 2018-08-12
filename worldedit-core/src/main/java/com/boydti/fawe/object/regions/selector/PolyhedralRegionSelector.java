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

package com.boydti.fawe.object.regions.selector;

import com.boydti.fawe.object.regions.PolyhedralRegion;
import com.boydti.fawe.object.regions.Triangle;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.cui.CUIRegion;
import com.sk89q.worldedit.internal.cui.SelectionPointEvent;
import com.sk89q.worldedit.internal.cui.SelectionPolygonEvent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.world.World;

import java.util.*;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a {@code PolyhedralRegion} from a user's selections.
 */
public class PolyhedralRegionSelector implements RegionSelector, CUIRegion {

    private final transient PolyhedralRegion region;
    private transient BlockVector pos1;

    /**
     * Create a new selector with a {@code null} world.
     */
    public PolyhedralRegionSelector() {
        this((World) null);
    }

    /**
     * Create a new selector.
     *
     * @param world the world, which may be {@code null}
     */
    public PolyhedralRegionSelector(@Nullable World world) {
        region = new PolyhedralRegion(world);
    }

    @Override
    public List<Vector> getVerticies() {
        return new ArrayList<>(region.getVertices());
    }

    @Nullable
    @Override
    public World getWorld() {
        return region.getWorld();
    }

    @Override
    public void setWorld(@Nullable World world) {
        region.setWorld(world);
    }

    @Override
    public boolean selectPrimary(Vector position, SelectorLimits limits) {
        checkNotNull(position);
        clear();
        pos1 = position.toBlockVector();
        return region.addVertex(position);
    }

    @Override
    public boolean selectSecondary(Vector position, SelectorLimits limits) {
        checkNotNull(position);

        Optional<Integer> vertexLimit = limits.getPolyhedronVertexLimit();

        if (vertexLimit.isPresent() && region.getVertices().size() > vertexLimit.get()) {
            return false;
        }

        return region.addVertex(position);
    }

    @Override
    public BlockVector getPrimaryPosition() throws IncompleteRegionException {
        return pos1;
    }

    @Override
    public Region getRegion() throws IncompleteRegionException {
        if (!region.isDefined()) {
            throw new IncompleteRegionException();
        }

        return region;
    }

    @Override
    public Region getIncompleteRegion() {
        return region;
    }

    @Override
    public boolean isDefined() {
        return region.isDefined();
    }

    @Override
    public int getArea() {
        return region.getArea();
    }

    @Override
    public void learnChanges() {
        pos1 = region.getVertices().iterator().next().toBlockVector();
    }

    @Override
    public void clear() {
        region.clear();
    }

    @Override
    public String getTypeName() {
        return "Polyhedron";
    }

    @Override
    public List<String> getInformationLines() {
        List<String> ret = new ArrayList<String>();

        ret.add("Vertices: " + region.getVertices().size());
        ret.add("Triangles: " + region.getTriangles().size());

        return ret;
    }


    @Override
    public void explainPrimarySelection(Actor player, LocalSession session, Vector pos) {
        checkNotNull(player);
        checkNotNull(session);
        checkNotNull(pos);

        session.describeCUI(player);

        player.print("Started new selection with vertex " + pos + ".");
    }

    @Override
    public void explainSecondarySelection(Actor player, LocalSession session, Vector pos) {
        checkNotNull(player);
        checkNotNull(session);
        checkNotNull(pos);

        session.describeCUI(player);

        player.print("Added vertex " + pos + " to the selection.");
    }

    @Override
    public void explainRegionAdjust(Actor player, LocalSession session) {
        checkNotNull(player);
        checkNotNull(session);
        session.describeCUI(player);
    }

    @Override
    public int getProtocolVersion() {
        return 3;
    }

    @Override
    public String getTypeID() {
        return "polyhedron";
    }

    @Override
    public void describeCUI(LocalSession session, Actor player) {
        checkNotNull(player);
        checkNotNull(session);

        Collection<Vector> vertices = region.getVertices();
        Collection<Triangle> triangles = region.getTriangles();

        Map<Vector, Integer> vertexIds = new HashMap<Vector, Integer>(vertices.size());
        int lastVertexId = -1;
        for (Vector vertex : vertices) {
            vertexIds.put(vertex, ++lastVertexId);
            session.dispatchCUIEvent(player, new SelectionPointEvent(lastVertexId, vertex, getArea()));
        }

        for (Triangle triangle : triangles) {
            final int[] v = new int[3];
            for (int i = 0; i < 3; ++i) {
                v[i] = vertexIds.get(triangle.getVertex(i));
            }
            session.dispatchCUIEvent(player, new SelectionPolygonEvent(v));
        }
    }

    @Override
    public String getLegacyTypeID() {
        return "cuboid";
    }

    @Override
    public void describeLegacyCUI(LocalSession session, Actor player) {
        checkNotNull(player);
        checkNotNull(session);

        if (isDefined()) {
            session.dispatchCUIEvent(player, new SelectionPointEvent(0, region.getMinimumPoint(), getArea()));
            session.dispatchCUIEvent(player, new SelectionPointEvent(1, region.getMaximumPoint(), getArea()));
        }
    }

}
