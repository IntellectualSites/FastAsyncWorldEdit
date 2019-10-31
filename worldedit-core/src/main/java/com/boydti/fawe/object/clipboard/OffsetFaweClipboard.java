package com.boydti.fawe.object.clipboard;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class OffsetClipboard extends DelegateClipboard {
    private final int ox, oy, oz;

    public OffsetClipboard(Clipboard parent, int ox, int oy, int oz) {
        super(parent);
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
    }

    @Override
    public Region getRegion() {
        return parent.getRegion();
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        return super.getBlock(x + ox, y + oy, z + oz);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return super.setBlock(ox + x, oy + y, oz + z, block);
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biome) {
        return super.setBiome(ox + x, oz + z, biome);
    }

    @Override
    public BiomeType getBiome(int x, int z) {
        return super.getBiome(ox + x, oz + z);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        return super.setTile(ox + x, oy + y, oz + z, tag);
    }

    @Override
    public void forEach(final BlockReader task, boolean air) {
        super.forEach(new BlockReader() {
            @Override
            public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
                task.run(x - ox, y - oy, z - oz, block);
            }
        }, air);
    }
}
