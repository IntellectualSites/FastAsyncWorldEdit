/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether another mask tests true for a position that is offset
 * a given vector.
 */
public class OffsetMask extends AbstractMask {

    //FAWE start - ignore resultant position outside world height range
    private final int minY;
    private final int maxY;
    //FAWE end
    private Mask mask;
    private BlockVector3 offset;

    /**
     * Create a new instance.
     *
     * @param mask   the mask
     * @param offset the offset
     * @deprecated use {@link OffsetMask#OffsetMask(Mask, BlockVector3, int, int)}
     */
    @Deprecated
    public OffsetMask(Mask mask, BlockVector3 offset) {
        this(mask, offset,
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMinY(),
                WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMaxY()
        );
    }

    /**
     * Create a new instance.
     *
     * @param mask   the mask
     * @param offset the offset
     * @param minY   minimum allowable y value to be set. Inclusive.
     * @param maxY   maximum allowable y value to be set. Inclusive.
     */
    //FAWE start - ignore resultant position outside world height range
    public OffsetMask(Mask mask, BlockVector3 offset, int minY, int maxY) {
        checkNotNull(mask);
        checkNotNull(offset);
        this.mask = mask;
        this.offset = offset;
        this.minY = minY;
        this.maxY = maxY;
        //FAWE end
    }

    /**
     * Get the mask.
     *
     * @return the mask
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Set the mask.
     *
     * @param mask the mask
     */
    public void setMask(Mask mask) {
        checkNotNull(mask);
        this.mask = mask;
    }

    /**
     * Get the offset.
     *
     * @return the offset
     */
    public BlockVector3 getOffset() {
        return offset;
    }

    /**
     * Set the offset.
     *
     * @param offset the offset
     */
    public void setOffset(BlockVector3 offset) {
        checkNotNull(offset);
        this.offset = offset;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        //FAWE start - ignore resultant position outside world height range
        BlockVector3 testPos = vector.add(offset);
        if (testPos.y() < minY || testPos.y() > maxY) {
            return false;
        }
        return getMask().test(testPos);
        //FAWE end
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        Mask2D childMask = getMask().toMask2D();
        if (childMask != null) {
            return new OffsetMask2D(childMask, getOffset().toBlockVector2());
        } else {
            return null;
        }
    }

    //FAWE start
    @Override
    public Mask copy() {
        return new OffsetMask(mask.copy(), offset.toImmutable(), minY, maxY);
    }
    //FAWE end

}
