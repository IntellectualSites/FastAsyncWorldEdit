package com.fastasyncworldedit.core.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

public record Either<L, R>(@Nullable L left, @Nullable R right) {

    public <NL, NR> Either<NL, NR> map(@NonNull Function<L, NL> leftMapper, @NonNull Function<R, NR> rightMapper) {
        if (left() != null) {
            return new Either<>(leftMapper.apply(left()), null);
        }
        if (right() != null) {
            return new Either<>(null, rightMapper.apply(right()));
        }
        return new Either<>(null, null);
    }

    public <Result> Result accept(
            @NonNull Function<L, Result> lConsumer, @NonNull Function<R, Result> rConsumer,
            @NonNull Result nullFallback
    ) {
        if (left() != null) {
            return lConsumer.apply(left());
        }
        if (right() != null) {
            return rConsumer.apply(right());
        }
        return nullFallback;
    }

    public static <SL, SR> Either<SL, SR> empty() {
        return new Either<>(null, null);
    }

}
