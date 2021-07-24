package com.fastasyncworldedit.core.function;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Passes calls to {@link #apply(BlockVector3)} to the
 * delegate {@link RegionFunction} if they
 * match the given mask.
 */
public class RegionMaskTestFunction implements RegionFunction {

    private final RegionFunction pass;
    private final RegionFunction fail;
    private final Mask mask;

    /**
     * Create a new masking filter.
     *
     * @param mask    the mask
     * @param failure the function
     */
    public RegionMaskTestFunction(Mask mask, RegionFunction success, RegionFunction failure) {
        checkNotNull(success);
        checkNotNull(failure);
        checkNotNull(mask);
        this.pass = success;
        this.fail = failure;
        this.mask = mask;
    }

    @Override
    public boolean apply(BlockVector3 position) throws WorldEditException {
        if (mask.test(position)) {
            return pass.apply(position);
        } else {
            return fail.apply(position);
        }
    }

}
