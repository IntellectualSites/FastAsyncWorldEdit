package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BaseBiome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldCopyClipboard extends ReadOnlyClipboard {

    public final int mx, my, mz;
    private final boolean hasBiomes;
    private final boolean hasEntities;
    private MutableBlockVector2D mutableBlockVector2D = new MutableBlockVector2D();
    public final Extent extent;

    public WorldCopyClipboard(Extent editSession, Region region) {
        this(editSession, region, true, false);
    }

    public WorldCopyClipboard(Extent editSession, Region region, boolean hasEntities, boolean hasBiomes) {
        super(region);
        this.hasBiomes = hasBiomes;
        this.hasEntities = hasEntities;
        final Vector origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
        this.extent = editSession;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return extent.getLazyBlock(mx + x, my + y, mz + z);
    }

    public BlockState getBlockAbs(int x, int y, int z) {
        return extent.getLazyBlock(x, y, z);
    }

    @Override
    public BaseBiome getBiome(int x, int z) {
        return extent.getBiome(mutableBlockVector2D.setComponents(mx + x, mz + z));
    }

    @Override
    public List<? extends Entity> getEntities() {
        if (!hasEntities) return new ArrayList<>();
        return extent.getEntities(getRegion());
    }

    @Override
    public boolean hasBiomes() {
        return hasBiomes;
    }

    @Override
    public void forEach(BlockReader task, boolean air) {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        final Vector pos = new Vector();
        if (region instanceof CuboidRegion) {
            if (air) {
                ((CuboidRegion) region).setUseOldIterator(true);
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(Vector pos) throws WorldEditException {
                        BlockState block = getBlockAbs(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
                        int x = pos.getBlockX() - mx;
                        int y = pos.getBlockY() - my;
                        int z = pos.getBlockZ() - mz;
                        CompoundTag tag = block.getNbtData();
                        if (tag != null) {
                            Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                            values.put("x", new IntTag(x));
                            values.put("y", new IntTag(y));
                            values.put("z", new IntTag(z));
                        }
                        task.run(x, y, z, block);
                        return true;
                    }
                }, extent instanceof EditSession ? (EditSession) extent : null);
                Operations.completeBlindly(visitor);
            } else {
                CuboidRegion cuboidEquivalent = new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint());
                cuboidEquivalent.setUseOldIterator(true);
                RegionVisitor visitor = new RegionVisitor(cuboidEquivalent, new RegionFunction() {
                    @Override
                    public boolean apply(Vector pos) throws WorldEditException {
                        int x = pos.getBlockX() - mx;
                        int y = pos.getBlockY() - my;
                        int z = pos.getBlockZ() - mz;
                        if (region.contains(pos)) {
                            BlockState block = getBlockAbs(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
                            CompoundTag tag = block.getNbtData();
                            if (tag != null) {
                                Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                values.put("x", new IntTag(x));
                                values.put("y", new IntTag(y));
                                values.put("z", new IntTag(z));
                            }
                            if (!block.getBlockType().getMaterial().isAir()) {
                                task.run(x, y, z, block);
                            }
                        } else {
//                            task.run(x, y, z, EditSession.nullBlock);
                        }
                        return true;
                    }
                }, extent instanceof EditSession ? (EditSession) extent : null);
                Operations.completeBlindly(visitor);
            }
        } else {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                pos.mutY(y);
                int yy = pos.getBlockY() - my;
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    pos.mutZ(z);
                    int zz = pos.getBlockZ() - mz;
                    for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                        pos.mutX(x);
                        int xx = pos.getBlockX() - mx;
                        if (region.contains(pos)) {
                            BlockState block = getBlockAbs(x, y, z);
                            if (!air && block.getBlockType().getMaterial().isAir()) {
                                continue;
                            }
                            CompoundTag tag = block.getNbtData();
                            if (tag != null) {
                                Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                                values.put("x", new IntTag(xx));
                                values.put("y", new IntTag(yy));
                                values.put("z", new IntTag(zz));
                            }
                            task.run(xx, yy, zz, block);
                        } else if (air) {
                            task.run(xx, yy, zz, EditSession.nullBlock);
                        }
                    }
                }
            }
        }
    }
}
