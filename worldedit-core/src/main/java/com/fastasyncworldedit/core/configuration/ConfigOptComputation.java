package com.fastasyncworldedit.core.configuration;

sealed abstract class ConfigOptComputation<R> permits
        ConfigOptComputation.THREAD_TARGET_SIZE_COMPUTATION {

    public abstract R apply(Object val);

    static final class THREAD_TARGET_SIZE_COMPUTATION extends ConfigOptComputation<Integer> {

        @Override
        public Integer apply(Object val) {
            return 100 * 2 / (Integer) val;
        }

    }

}
