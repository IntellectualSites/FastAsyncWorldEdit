package com.fastasyncworldedit.core.history.change;

import com.sk89q.worldedit.history.change.Change;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @since TODO
 */
@ApiStatus.Internal
public interface ChangePopulator<C extends Change> {

    static <C extends Change> ChangePopulator<C> empty() {
        class Empty implements ChangePopulator<C> {
            private static final Empty EMPTY = new Empty();

            @Override
            public @NotNull C create() {
                throw new UnsupportedOperationException("empty");
            }

            @Override
            public @Nullable C populate(@NotNull final C change) {
                return null;
            }

            @Override
            public @Nullable C updateOrCreate(@Nullable final Change change) {
                return null;
            }

            @Override
            public boolean accepts(final Change change) {
                return false;
            }
        }
        return Empty.EMPTY;
    }

    @SuppressWarnings("unchecked")
    default @NotNull C update(@Nullable Change before) {
        if (accepts(before)) {
            return (C) before;
        }
        return create();
    }

    @NotNull
    C create();

    @Nullable
    default C updateOrCreate(@Nullable Change change) {
        C u = update(change);
        return populate(u);
    }

    @Nullable
    C populate(@NotNull C change);

    @Contract("null->false")
    boolean accepts(Change change);

}
