package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.IOException;

public class BiomePattern extends ExistingPattern {
    private transient MutableBlockVector2D mutable = new MutableBlockVector2D();
    private final BaseBiome biome;

    public BiomePattern(Extent extent, BaseBiome biome) {
        super(extent);
        this.biome = biome;
    }

    @Override
    public BaseBlock apply(Vector position) {
        throw new BiomePatternException();
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector getPosition) throws WorldEditException {
        return extent.setBiome(set.getBlockX(), set.getBlockY(), set.getBlockZ(), biome);
    }

    public class BiomePatternException extends RuntimeException {
        private BiomePatternException() {
        }

        public BiomePattern getPattern() {
            return BiomePattern.this;
        }

        public BaseBiome getBiome() {
            return biome;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        mutable = new MutableBlockVector2D();
    }
}
