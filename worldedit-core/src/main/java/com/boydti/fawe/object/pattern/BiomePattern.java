package com.boydti.fawe.object.pattern;

import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector2;
import com.sk89q.worldedit.world.biome.BiomeType;
import java.io.IOException;

public class BiomePattern extends ExistingPattern {
    private final BiomeType biome;

    public BiomePattern(Extent extent, BiomeType biome) {
        super(extent);
        this.biome = biome;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        throw new BiomePatternException();
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 get, BlockVector3 set) throws WorldEditException {
        return set.setBiome(extent, biome);
    }

    public class BiomePatternException extends RuntimeException {
        private BiomePatternException() {
        }

        public BiomePattern getPattern() {
            return BiomePattern.this;
        }

        public BiomeType getBiome() {
            return biome;
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
