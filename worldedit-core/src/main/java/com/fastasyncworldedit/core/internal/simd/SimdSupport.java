package com.fastasyncworldedit.core.internal.simd;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.filter.block.DelegateFilter;
import com.fastasyncworldedit.core.function.mask.InverseMask;
import com.fastasyncworldedit.core.function.mask.SingleBlockStateMask;
import com.fastasyncworldedit.core.queue.Filter;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.InverseSingleBlockStateMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class SimdSupport {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
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
            LOGGER.warn("""
                    FAWE use-vector-api is enabled but --add-modules=jdk.incubator.vector is not set.
                    Vector instructions will not be used.
                    """);
        } else if (VECTOR_API_PRESENT && !Settings.settings().EXPERIMENTAL.USE_VECTOR_API) {
            LOGGER.warn("""
                    The server is running with the --add-modules=jdk.incubator.vector option.
                    FAWE can use vector instructions, but it is disabled in the config.
                    Enable use-vector-api to benefit from vector instructions with FAWE.\
                    """);
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
            case ExistingBlockMask ignored -> vectorizedTargetMaskNonAir();
            case InverseMask inverse -> {
                final VectorizedMask base = vectorizedTargetMask(inverse.inverse());
                if (base == null) {
                    yield null;
                }
                yield (set, get, species) -> base.compareVector(set, get, species).not();
            }
            default -> null;
        };
    }

    private static <T> VectorizedMask vectorizedTargetMaskNonAir() {
        // everything > VOID_AIR is not air
        return (set, get, species) -> get.get(species).compare(VectorOperators.UGE, BlockTypesCache.ReservedIDs.VOID_AIR);
    }

    private static <T> VectorizedMask vectorizedTargetMask(char ordinal) {
        return (set, get, species) -> get.get(species).compare(VectorOperators.EQ, (short) ordinal);
    }

    private static <T> VectorizedMask vectorizedTargetMaskInverse(char ordinal) {
        return (set, get, species) -> get.get(species).compare(VectorOperators.NE, (short) ordinal);
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
        public Filter newInstance(final Filter other) {
            return new VectorizedPattern<>(other, ordinal);
        }

        @Override
        public void applyVector(final VectorFacade get, final VectorFacade set, final VectorMask<Integer> mask) {
            IntVector s = set.getOrZero(mask.vectorSpecies());
            // only change the lanes the mask dictates us to change, keep the rest
            s = s.blend(IntVector.broadcast(s.species(), ordinal), mask);
            set.setOrIgnore(s);
        }

    }

}
