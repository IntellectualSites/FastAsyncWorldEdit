package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public interface VectorizedMask {

    default void processChunks(IChunk chunk, IChunkGet get, IChunkSet set) {
        VectorFacade setFassade = new VectorFacade(set);
        VectorFacade getFassade = new VectorFacade(get);
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            setFassade.setLayer(layer);
            getFassade.setLayer(layer);
            final char[] sectionSet = set.loadIfPresent(layer);
            if (sectionSet == null) {
                continue;
            }
            setFassade.setData(sectionSet);
            processSection(layer, setFassade, getFassade);
        }
    }

    default void processSection(int layer, VectorFacade set, VectorFacade get) {
        final VectorSpecies<Short> species = ShortVector.SPECIES_PREFERRED;
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
    default void processVector(VectorFacade set, VectorFacade get, VectorSpecies<Short> species) {
        ShortVector s = set.getOrZero(species);
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
    VectorMask<Short> compareVector(VectorFacade set, VectorFacade get, VectorSpecies<Short> species);

}
