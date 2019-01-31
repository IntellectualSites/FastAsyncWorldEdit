/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.session;

import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds an operation to paste the contents of a clipboard.
 */
public class PasteBuilder {

    private final Clipboard clipboard;
    private final Transform transform;
    private final Extent targetExtent;

    private BlockVector3 to = BlockVector3.ZERO;
    private boolean ignoreAirBlocks;
    private boolean ignoreBiomes;
    private boolean ignoreEntities;
    private RegionFunction canApply;

    /**
     * Create a new instance.
     *
     * @param holder          the clipboard holder
     * @param targetExtent    an extent
     */
    public PasteBuilder(ClipboardHolder holder, Extent targetExtent) {
        checkNotNull(holder);
        checkNotNull(targetExtent);
        this.clipboard = holder.getClipboard();
        this.transform = holder.getTransform();
        this.targetExtent = targetExtent;
    }

    /**
     * Set the target location.
     *
     * @param to the target location
     * @return this builder instance
     */
    public PasteBuilder to(BlockVector3 to) {
        this.to = to;
        return this;
    }

    /**
     * Set whether air blocks in the source are skipped over when pasting.
     *
     * @return this builder instance
     */
    public PasteBuilder ignoreAirBlocks(boolean ignoreAirBlocks) {
        this.ignoreAirBlocks = ignoreAirBlocks;
        return this;
    }

    public PasteBuilder ignoreBiomes(boolean ignoreBiomes) {
        this.ignoreBiomes = ignoreBiomes;
        return this;
    }

    public PasteBuilder ignoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
        return this;
    }

    public PasteBuilder filter(RegionFunction function) {
        this.canApply = function;
        return this;
    }

    /**
     * Build the operation.
     *
     * @return the operation
     */
    public Operation build() {
        Extent extent = clipboard;
        if (!transform.isIdentity()) {
            extent = new BlockTransformExtent(extent, transform);
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), targetExtent, to);
        copy.setTransform(transform);
        copy.setCopyingEntities(!ignoreEntities);
        copy.setCopyBiomes((!ignoreBiomes) && (!(clipboard instanceof BlockArrayClipboard) || ((BlockArrayClipboard) clipboard).IMP.hasBiomes()));
        if (this.canApply != null) {
            copy.setFilterFunction(this.canApply);
        }
        if (targetExtent instanceof EditSession) {
            Mask sourceMask = ((EditSession) targetExtent).getSourceMask();
            if (sourceMask != null) {
                new MaskTraverser(sourceMask).reset(extent);
                copy.setSourceMask(sourceMask);
                ((EditSession) targetExtent).setSourceMask(null);
            }
        }
        if (ignoreAirBlocks) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        System.out.println("PasteBuilder info: extent: " + extent.toString() + "; copy: " + copy + "; transform: " + transform);
        return copy;
    }


}
