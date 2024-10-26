package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.extent.filter.block.DataArrayFilterBlock;
import com.fastasyncworldedit.core.queue.Filter;
import com.fastasyncworldedit.core.queue.implementation.blocks.DataArray;
import com.sk89q.worldedit.extent.Extent;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

public class VectorizedCharFilterBlock extends DataArrayFilterBlock {

    public VectorizedCharFilterBlock(final Extent extent) {
        super(extent);
    }

    @Override
    public synchronized void filter(final Filter filter) {
        if (!(filter instanceof VectorizedFilter<?> vecFilter)) {
            throw new IllegalStateException("Unexpected VectorizedCharFilterBlock " + filter);
        }
        filter(vecFilter);
    }

    private <V> void filter(VectorizedFilter<V> vecFilter) {
        final VectorSpecies<V> species = DataArray.preferredSpecies();
        // TODO can we avoid eager initSet?
        initSet(); // set array is null before
        DataArray setArr = this.setArr;
        assert setArr != null;
        DataArray getArr = this.getArr;
        VectorMask<V> affectAll = species.maskAll(true);
        setArr.processSet(getArr, (Vector<V> set, Vector<V> get) -> vecFilter.applyVector(get, set, affectAll));
    }

}
