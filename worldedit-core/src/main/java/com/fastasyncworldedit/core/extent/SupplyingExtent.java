package com.fastasyncworldedit.core.extent;

import com.sk89q.worldedit.extent.Extent;

import java.util.function.Supplier;

/**
 * An extent that delegates actions to another extent that may change at any time.
 */
public final class SupplyingExtent extends PassthroughExtent {

    private final Supplier<Extent> extentSupplier;

    public SupplyingExtent(Supplier<Extent> extentSupplier) {
        super(extentSupplier.get());
        this.extentSupplier = extentSupplier;
    }

    @Override
    public Extent getExtent() {
        return this.extentSupplier.get();
    }

}
