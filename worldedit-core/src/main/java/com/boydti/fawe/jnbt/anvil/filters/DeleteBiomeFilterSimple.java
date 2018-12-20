package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class DeleteBiomeFilterSimple extends MCAFilterCounter {
    private final int id;

    public DeleteBiomeFilterSimple(BaseBiome biome) {
        this.id = biome.getId();
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        if ((chunk.biomes[0] & 0xFF) == id) {
            chunk.setDeleted(true);
            cache.add(Character.MAX_VALUE);
        }
        return null;
    }
}
