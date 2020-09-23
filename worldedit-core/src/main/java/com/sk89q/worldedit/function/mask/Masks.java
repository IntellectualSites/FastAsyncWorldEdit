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

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Various utility functions related to {@link Mask} and {@link Mask2D}.
 */
public final class Masks {

    static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();
    static final AlwaysFalse ALWAYS_FALSE = new AlwaysFalse();

    private Masks() {
    }

    public static boolean isNull(Mask mask) {
        return mask == null || mask == ALWAYS_TRUE;
    }

    /**
     * Return a 3D mask that always returns true.
     *
     * @return a mask
     */
    public static Mask alwaysTrue() {
        return ALWAYS_TRUE;
    }

    public static Mask alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Return a 2D mask that always returns true.
     *
     * @return a mask
     */
    public static Mask2D alwaysTrue2D() {
        return ALWAYS_TRUE;
    }

    /**
     * Negate the given mask.
     *
     * @param mask the mask
     * @return a new mask
     */
    public static Mask negate(final Mask mask) {
        return mask.inverse();
    }

    /**
     * Negate the given mask.
     *
     * @param mask the mask
     * @return a new mask
     */
    public static Mask2D negate(final Mask2D mask) {
        if (mask instanceof AlwaysTrue) {
            return ALWAYS_FALSE;
        } else if (mask instanceof AlwaysFalse) {
            return ALWAYS_TRUE;
        }

        checkNotNull(mask);
        return new AbstractMask2D() {
            @Override
            public boolean test(BlockVector2 vector) {
                return !mask.test(vector);
            }

            @Override
            public Mask2D copy2D() {
                return Masks.negate(mask.copy2D());
            }
        };
    }

    /**
     * Return a 3-dimensional version of a 2D mask.
     *
     * @param mask the mask to make 3D
     * @return a 3D mask
     */
    public static Mask asMask(final Mask2D mask) {
        return new AbstractMask() {
            @Override
            public boolean test(BlockVector3 vector) {
                return mask.test(vector.toBlockVector2());
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return mask;
            }

            @Override
            public Mask copy() {
                return Masks.asMask(mask.copy2D());
            }
        };
    }

    protected static class AlwaysTrue implements Mask, Mask2D {
        @Override
        public boolean test(BlockVector3 vector) {
            return true;
        }

        @Override
        public boolean test(BlockVector2 vector) {
            return true;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }

        @Override
        public Mask tryCombine(Mask other) {
            return other;
        }

        @Override
        public Mask tryOr(Mask other) {
            return this;
        }

        // No need to properly clone an always true mask
        @Override
        public Mask copy() {
            return new AlwaysTrue();
        }

        // No need to properly clone an always true mask
        @Override
        public Mask2D copy2D() {
            return new AlwaysTrue();
        }

    }

    protected static class AlwaysFalse implements Mask, Mask2D {
        @Override
        public boolean test(BlockVector3 vector) {
            return false;
        }

        @Override
        public boolean test(BlockVector2 vector) {
            return false;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }

        @Override
        public Mask tryCombine(Mask other) {
            return this;
        }

        @Override
        public Mask tryOr(Mask other) {
            return other;
        }

        // No need to properly clone an always false mask
        @Override
        public Mask copy() {
            return new AlwaysFalse();
        }

        // No need to properly clone an always true mask
        @Override
        public Mask2D copy2D() {
            return new AlwaysFalse();
        }

    }

}
