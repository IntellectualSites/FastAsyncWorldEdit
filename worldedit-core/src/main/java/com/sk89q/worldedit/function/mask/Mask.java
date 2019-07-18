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

package com.sk89q.worldedit.function.mask;

import com.boydti.fawe.beta.DelegateFilter;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.FilterBlock;
import com.sk89q.minecraft.util.commands.Link;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

/**
 * Tests whether a given vector meets a criteria.
 */
@Link(clazz = UtilityCommands.class, value = "masks")
public interface Mask {

    /**
     * Returns true if the criteria is met.
     *
     * @param vector the vector to test
     * @return true if the criteria is met
     */
    boolean test(BlockVector3 vector);

    default Filter toFilter(Runnable run) {
        return new Filter() {
            @Override
            public void applyBlock(FilterBlock block) {
                if (test(block)) {
                    run.run();
                }
            }
        };
    }

    default <T extends Filter> DelegateFilter<T> toFilter(T filter) {
        return new DelegateFilter<T>(filter) {
            @Override
            public void applyBlock(FilterBlock block) {
                if (test(block)) {
                    filter.applyBlock(block);
                }
            }
        };
    }

    /**
     * Get the 2D version of this mask if one exists.
     *
     * @return a 2D mask version or {@code null} if this mask can't be 2D
     */
    @Nullable
    default Mask2D toMask2D() {
        return null;
    }

    /**
     * Returns null if no optimization took place
     * otherwise a new/same mask
     * @return
     */
    default Mask tryOptimize() {
        return null;
    }

    default Mask tryCombine(Mask other) {
        return null;
    }

    default Mask tryOr(Mask other) {
        return null;
    }

    default Mask optimize() {
        Mask value = tryOptimize();
        return value == null ? this : value;
    }

    default Mask and(Mask other) {
        Mask value = and(other);
        return value == null ? new MaskIntersection(this, other) : value;
    }

    default Mask or(Mask other) {
        Mask value = or(other);
        return value == null ? new MaskUnion(this, other) : value;
    }

    default Mask inverse() {
        if (this instanceof Masks.AlwaysTrue) {
            return Masks.ALWAYS_FALSE;
        } else if (this instanceof Masks.AlwaysFalse) {
            return Masks.ALWAYS_TRUE;
        }
        return new InverseMask(this);
    }
}
