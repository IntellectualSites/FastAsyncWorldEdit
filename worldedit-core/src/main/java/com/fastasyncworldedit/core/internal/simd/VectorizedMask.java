package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public interface VectorizedMask {

    default void processChunks(IChunk chunk, IChunkGet get, IChunkSet set) {
        VectorFacade setFacade = new VectorFacade(set);
        VectorFacade getFacade = new VectorFacade(get);
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            setFacade.setLayer(layer);
            getFacade.setLayer(layer);
            final DataArray sectionSet = set.loadIfPresent(layer);
            if (sectionSet == null) {
                continue;
            }
            setFacade.setData(sectionSet);
            processSection(layer, setFacade, getFacade);
        }
    }

    default void processSection(int layer, VectorFacade set, VectorFacade get) {
        final VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
        // assume that chunk sections have length 16 * 16 * 16 == 4096
        for (int i = 0; i < 4096; i += species.length()) {
            set.setIndex(i);
            get.setIndex(i);
            processVector(set, get, species);

        }
    }

    /**
     * Clears all blocks that aren't covered by the mask.
     *
     * @param set     the set vector
     * @param get     the get vector
     * @param species the species to use
     */
    default void processVector(VectorFacade set, VectorFacade get, VectorSpecies<Integer> species) {
        IntVector s = set.getOrZero(species);
        s = s.blend(BlockTypesCache.ReservedIDs.__RESERVED__, compareVector(set, get, species).not());
        set.setOrIgnore(s);
    }

    /**
     * {@return a mask with all lanes set that match this mask}
     *
     * @param set     the set vector
     * @param get     the get vector
     * @param species the species to use
     */
    VectorMask<Integer> compareVector(VectorFacade set, VectorFacade get, VectorSpecies<Integer> species);

}
