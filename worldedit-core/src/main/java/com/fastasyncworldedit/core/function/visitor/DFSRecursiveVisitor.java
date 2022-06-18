package com.fastasyncworldedit.core.function.visitor;

import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of an {@link DFSVisitor} that uses a mask to determine where a block should be visited. The visit is
 * performed using a {@link com.fastasyncworldedit.core.math.MutableBlockVector3}
 */
public class DFSRecursiveVisitor extends DFSVisitor {

    private final Mask mask;

    public DFSRecursiveVisitor(final Mask mask, final RegionFunction function) {
        this(mask, function, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Create a new recursive visitor.
     *
     * @param mask     the mask
     * @param function the function
     */
    public DFSRecursiveVisitor(
            final Mask mask, final RegionFunction function, int maxDepth,
            int maxBranching
    ) {
        super(function, maxDepth, maxBranching);
        checkNotNull(mask);
        this.mask = mask;
    }

    @Override
    public boolean isVisitable(final BlockVector3 from, final BlockVector3 to) {
        return this.mask.test(to);
    }

}
