package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldCopyClipboard extends ReadOnlyClipboard {

    public final int mx, my, mz;
    private final boolean hasBiomes;
    private final boolean hasEntities;
    private MutableBlockVector2 MutableBlockVector2 = new MutableBlockVector2();
    public final Extent extent;

    public WorldCopyClipboard(Extent editSession, Region region) {
        this(editSession, region, true, false);
    }

    public WorldCopyClipboard(Extent editSession, Region region, boolean hasEntities, boolean hasBiomes) {
        super(region);
        this.hasBiomes = hasBiomes;
        this.hasEntities = hasEntities;
        final BlockVector3 origin = region.getMinimumPoint();
        this.mx = origin.getBlockX();
        this.my = origin.getBlockY();
        this.mz = origin.getBlockZ();
        this.extent = editSession;
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        return extent.getFullBlock(BlockVector3.at(mx + x, my + y, mz + z));
    }

    public BaseBlock getBlockAbs(int x, int y, int z) {
        return extent.getFullBlock(BlockVector3.at(x, y, z));
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        return extent.getBiome(MutableBlockVector2.setComponents(mx + x, mz + z));
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
    	BlockVector3 min = region.getMinimumPoint();
    	BlockVector3 max = region.getMaximumPoint();
        MutableBlockVector3 pos = new MutableBlockVector3();
        if (region instanceof CuboidRegion) {
            if (air) {
                ((CuboidRegion) region).setUseOldIterator(true);
                RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                    @Override
                    public boolean apply(BlockVector3 pos) throws WorldEditException {
                        BaseBlock block = getBlockAbs(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
                        int x = pos.getBlockX() - mx;
                        int y = pos.getBlockY() - my;
                        int z = pos.getBlockZ() - mz;
                        if (block.hasNbtData()) {
                            Map<String, Tag> values = ReflectionUtils.getMap(block.getNbtData().getValue());
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
                    public boolean apply(BlockVector3 pos) throws WorldEditException {
                        int x = pos.getBlockX() - mx;
                        int y = pos.getBlockY() - my;
                        int z = pos.getBlockZ() - mz;
                        if (region.contains(pos)) {
//                            BlockState block = getBlockAbs(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
                        	BaseBlock block = extent.getFullBlock(pos);
                            if (block.hasNbtData()) {
                                Map<String, Tag> values = ReflectionUtils.getMap(block.getNbtData().getValue());
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
//                            BlockState block = getBlockAbs(x, y, z);
                        	BaseBlock block = extent.getFullBlock(pos);
                            if (!air && block.getBlockType().getMaterial().isAir()) {
                                continue;
                            }
                            if (block.hasNbtData()) {
                                Map<String, Tag> values = ReflectionUtils.getMap(block.getNbtData().getValue());
                                values.put("x", new IntTag(xx));
                                values.put("y", new IntTag(yy));
                                values.put("z", new IntTag(zz));
                            }
                            task.run(xx, yy, zz, block);
                        } else if (air) {
                            task.run(xx, yy, zz, BlockTypes.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }
}
