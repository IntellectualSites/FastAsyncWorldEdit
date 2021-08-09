package com.fastasyncworldedit.core.regions.selector;

import com.fastasyncworldedit.core.regions.PolyhedralRegion;
import com.fastasyncworldedit.core.regions.Triangle;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.cui.CUIRegion;
import com.sk89q.worldedit.internal.cui.SelectionPointEvent;
import com.sk89q.worldedit.internal.cui.SelectionPolygonEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a {@code PolyhedralRegion} from a user's selections.
 */
public class PolyhedralRegionSelector implements RegionSelector, CUIRegion {

    private final transient PolyhedralRegion region;
    private transient BlockVector3 pos1;

    /**
     * Create a new selector with a {@code null} world.
     */
    public PolyhedralRegionSelector() {
        this(null);
    }

    /**
     * Create a new selector.
     *
     * @param world the world, which may be {@code null}
     */
    public PolyhedralRegionSelector(@Nullable World world) {
        region = new PolyhedralRegion(world);
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
    public boolean selectPrimary(BlockVector3 position, SelectorLimits limits) {
        checkNotNull(position);
        clear();
        pos1 = position;
        return region.addVertex(position);
    }

    @Override
    public boolean selectSecondary(BlockVector3 position, SelectorLimits limits) {
        checkNotNull(position);

        Optional<Integer> vertexLimit = limits.getPolyhedronVertexLimit();

        if (vertexLimit.isPresent() && region.getVertices().size() > vertexLimit.get()) {
            return false;
        }

        return region.addVertex(position);
    }

    @Override
    public BlockVector3 getPrimaryPosition() throws IncompleteRegionException {
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
        pos1 = region.getVertices().iterator().next();
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
        List<String> ret = new ArrayList<>();

        ret.add("Vertices: " + region.getVertices().size());
        ret.add("Triangles: " + region.getTriangles().size());

        return ret;
    }


    @Override
    public void explainPrimarySelection(Actor player, LocalSession session, BlockVector3 pos) {
        checkNotNull(player);
        checkNotNull(session);
        checkNotNull(pos);

        session.describeCUI(player);

        player.print(TextComponent.of("Started new selection with vertex " + pos + "."));
    }

    @Override
    public void explainSecondarySelection(Actor player, LocalSession session, BlockVector3 pos) {
        checkNotNull(player);
        checkNotNull(session);
        checkNotNull(pos);

        session.describeCUI(player);

        player.print(TextComponent.of("Added vertex " + pos + " to the selection."));
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

        Collection<BlockVector3> vertices = region.getVertices();
        Collection<Triangle> triangles = region.getTriangles();

        Map<BlockVector3, Integer> vertexIds = new HashMap<>(vertices.size());
        int lastVertexId = -1;
        for (BlockVector3 vertex : vertices) {
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

    @Override
    public List<BlockVector3> getVertices() {
        return new ArrayList<>(region.getVertices());
    }

}
