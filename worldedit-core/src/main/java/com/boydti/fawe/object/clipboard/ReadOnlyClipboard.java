package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.Fawe;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public abstract class ReadOnlyClipboard extends SimpleClipboard {
    public final Region region;

    public ReadOnlyClipboard(Region region) {
        super(region.getDimensions());
        this.region = region;
    }

    public static ReadOnlyClipboard of(final Region region) {
        return of(Request.request().getEditSession(), region);
    }

    public static ReadOnlyClipboard of(final Region region, boolean copyEntities, boolean copyBiomes) {
        EditSession es = Request.request().getEditSession();
        if (es == null) {
            throw new IllegalArgumentException("Please provide an EditSession");
        }
        return of(es, region, copyEntities, copyBiomes);
    }

    public static ReadOnlyClipboard of(Extent extent, final Region region) {
        Fawe.get().getQueueHandler().unCache();
        return of(() -> extent, region);
    }

    public static ReadOnlyClipboard of(Extent extent, final Region region, boolean copyEntities, boolean copyBiomes) {
        Fawe.get().getQueueHandler().unCache();
        return of(() -> extent, region, copyEntities, copyBiomes);
    }

    public static ReadOnlyClipboard of(Supplier<Extent> supplier, final Region region) {
        return of(supplier, region, true, false);
    }

    public static ReadOnlyClipboard of(Supplier<Extent> supplier, final Region region, boolean copyEntities, boolean copyBiomes) {
        return new WorldCopyClipboard(supplier, region, copyEntities, copyBiomes);
    }

    private static Supplier<Extent> supply() {
        World world = Request.request().getWorld();
        return () -> {
            EditSession current = Request.request().getEditSession();
            if (current != null) {
                if (current.getWorld().equals(world)) {
                    return current;
                }
                throw new UnsupportedOperationException("TODO: Cannot lazy copy accross worlds (bug jesse)");
            }
            throw new IllegalStateException("No world");
        };
    }

    @NotNull
    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public boolean setBiome(int x, int y, int z, BiomeType biome) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public abstract List<? extends Entity> getEntities();

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }

    @Override
    public void removeEntity(Entity entity) {
        throw new UnsupportedOperationException("Clipboard is immutable");
    }
}
