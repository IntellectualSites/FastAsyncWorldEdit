package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Various utility functions related to {@link Mask} and {@link Mask2D}.
 */
public final class Masks {

    protected static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();
    protected static final AlwaysFalse ALWAYS_FALSE = new AlwaysFalse();

    private Masks() {
    }

    public static boolean isNull(Mask mask) {
        return mask == null || mask == ALWAYS_TRUE;
    }

    /**
     * Return a 3D mask that always returns true;
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
     * Return a 2D mask that always returns true;
     *
     * @return a mask
     */
    public static Mask2D alwaysTrue2D() {
        return ALWAYS_TRUE;
    }

    /**
     * Negate the given mask.
     *
     * @param finalMask the mask
     * @return a new mask
     */
    public static Mask negate(final Mask finalMask) {
        return finalMask.inverse();
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
            public boolean test(Vector2D vector) {
                return !mask.test(vector);
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
            public boolean test(Vector vector) {
                return mask.test(vector.toVector2D());
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return mask;
            }
        };
    }

    protected static class AlwaysTrue implements Mask, Mask2D {
        @Override
        public boolean test(Vector vector) {
            return true;
        }

        @Override
        public boolean test(Vector2D vector) {
            return true;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }

        @Override
        public Mask and(Mask other) {
            return other;
        }

        @Override
        public Mask or(Mask other) {
            return other;
        }
    }

    protected static class AlwaysFalse implements Mask, Mask2D {
        @Override
        public boolean test(Vector vector) {
            return false;
        }

        @Override
        public boolean test(Vector2D vector) {
            return false;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }

        @Override
        public Mask and(Mask other) {
            return this;
        }

        @Override
        public Mask or(Mask other) {
            return other;
        }
    }


}