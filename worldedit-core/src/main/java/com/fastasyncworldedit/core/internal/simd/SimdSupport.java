package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.filter.block.DelegateFilter;
import com.fastasyncworldedit.core.function.mask.SingleBlockStateMask;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.function.mask.InverseSingleBlockStateMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import javax.annotation.Nullable;

public class SimdSupport {

    private static final boolean VECTOR_API_PRESENT;

    static {
        boolean vectorApiPresent = false;
        try {
            Class.forName("jdk.incubator.vector.Vector");
            vectorApiPresent = true;
        } catch (ClassNotFoundException ignored) {
        }
        VECTOR_API_PRESENT = vectorApiPresent;
        if (!VECTOR_API_PRESENT && Settings.settings().EXPERIMENTAL.USE_VECTOR_API) {
            LogManagerCompat.getLogger()
                    .warn("FAWE use-vector-api is enabled but --add-modules=jdk.incubator.vector is not set.");
        }
    }

    public static boolean useVectorApi() {
        return VECTOR_API_PRESENT && Settings.settings().EXPERIMENTAL.USE_VECTOR_API;
    }

    public static @Nullable VectorizedMask vectorizedTargetMask(Mask mask) {
        if (!useVectorApi()) {
            return null;
        }
        return switch (mask) {
            case SingleBlockStateMask single -> vectorizedTargetMask(single.getBlockState().getOrdinalChar());
            case InverseSingleBlockStateMask inverse -> vectorizedTargetMaskInverse(inverse.getBlockState().getOrdinalChar());
            default -> null;
        };
    }

    private static VectorizedMask vectorizedTargetMask(char ordinal) {
        return (set, get) -> get.compare(VectorOperators.EQ, (short) ordinal);
    }

    private static VectorizedMask vectorizedTargetMaskInverse(char ordinal) {
        return (set, get) -> get.compare(VectorOperators.NE, (short) ordinal);
    }

    public static @Nullable VectorizedFilter vectorizedPattern(Pattern pattern) {
        if (!useVectorApi()) {
            return null;
        }
        return switch (pattern) {
            case BaseBlock block -> {
                if (block.getNbtReference() == null) {
                    yield new VectorizedPattern<>(block, block.getOrdinalChar());
                }
                yield null;
            }
            case BlockStateHolder<?> blockStateHolder -> new VectorizedPattern<>(
                    blockStateHolder,
                    blockStateHolder.getOrdinalChar()
            );
            default -> null;
        };
    }

    private static final class VectorizedPattern<T extends Filter> extends DelegateFilter<T> implements VectorizedFilter {

        private final char ordinal;

        public VectorizedPattern(final T parent, char ordinal) {
            super(parent);
            this.ordinal = ordinal;
        }

        @Override
        public ShortVector applyVector(final ShortVector get, final ShortVector set) {
            return ShortVector.broadcast(ShortVector.SPECIES_PREFERRED, ordinal);
        }

        @Override
        public Filter newInstance(final Filter other) {
            return new VectorizedPattern<>(other, ordinal);
        }

    }

}
