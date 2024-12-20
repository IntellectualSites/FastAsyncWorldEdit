package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.extent.filter.block.CharFilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.extent.Extent;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public class VectorizedCharFilterBlock extends CharFilterBlock {

    public VectorizedCharFilterBlock(final Extent extent) {
        super(extent);
    }

    @Override
    public synchronized void filter(final Filter filter) {
        if (!(filter instanceof VectorizedFilter vecFilter)) {
            throw new IllegalStateException("Unexpected VectorizedCharFilterBlock " + filter);
        }
        final VectorSpecies<Short> species = ShortVector.SPECIES_PREFERRED;
        VectorFacade setFassade = new VectorFacade(this.set);
        setFassade.setLayer(this.layer);
        VectorFacade getFassade = new VectorFacade(this.get);
        getFassade.setLayer(this.layer);
        getFassade.setData(this.getArr);
        // assume setArr.length == getArr.length == 4096
        VectorMask<Short> affectAll = species.maskAll(true);
        for (int i = 0; i < 4096; i += species.length()) {
            setFassade.setIndex(i);
            getFassade.setIndex(i);
            vecFilter.applyVector(getFassade, setFassade, affectAll);
        }
    }
}
