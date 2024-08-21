package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public interface VectorizedMask {

    default void processChunks(IChunk chunk, IChunkGet get, IChunkSet set) {
        for (int layer = get.getMinSectionPosition(); layer <= get.getMaxSectionPosition(); layer++) {
            final char[] sectionSet = set.loadIfPresent(layer);
            if (sectionSet == null) {
                continue;
            }
            final char[] sectionGet = get.load(layer);
            processSection(layer, sectionSet, sectionGet);
        }
    }

    default void processSection(int layer, char[] set, char[] get) {
        final VectorSpecies<Short> species = ShortVector.SPECIES_PREFERRED;
        for (int i = 0; i < set.length; i += species.length()) {
            ShortVector vectorSet = ShortVector.fromCharArray(species, set, i);
            ShortVector vectorGet = ShortVector.fromCharArray(species, get, i);
            vectorSet = processVector(vectorSet, vectorGet);
            vectorSet.intoCharArray(set, i);
        }
    }

    default ShortVector processVector(ShortVector set, ShortVector get) {
        return set.blend(0, compareVector(set, get).not());
    }

    VectorMask<Short> compareVector(ShortVector set, ShortVector get);

}
