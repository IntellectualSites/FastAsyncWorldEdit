package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_11;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueOutput;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ValueOutput implementation for direct LinBus interaction.
 * Basically a copy of {@link net.minecraft.world.level.storage.TagValueOutput}, but for Lin (with changes so it actually works).
 * <p>
 * Given LinBus is extremely immutable (except it's builders of course), this ValueOutput needs intermediate types and collection
 * maps to work. The ValueOutput expects mutability of it's underlying data structures (for example when creating lists or
 * child compounds as those are modified in subsequent method calls on the ValueOutput but <b>must</b> be reflected in their
 * parents).
 */
public class LinValueOutput implements ValueOutput {

    private final ProblemReporter problemReporter;
    private final DynamicOps<LinTag<?>> ops;
    private final LinCompoundTag.Builder builder;
    private final Map<String, PendingTagEntry> collector;

    LinValueOutput(final ProblemReporter reporter, final DynamicOps<LinTag<?>> ops, final LinCompoundTag.Builder output) {
        this.problemReporter = reporter;
        this.ops = ops;
        this.builder = output;
        this.collector = new LinkedHashMap<>();
    }

    public static LinValueOutput createWrappingWithContext(
            ProblemReporter problemReporter,
            HolderLookup.Provider lookup
    ) {
        return new LinValueOutput(problemReporter, lookup.createSerializationContext(LinOps.INSTANCE), LinCompoundTag.builder());
    }

    public static LinValueOutput createWithContext(ProblemReporter reporter, HolderLookup.Provider lookup) {
        return new LinValueOutput(reporter, lookup.createSerializationContext(LinOps.INSTANCE), LinCompoundTag.builder());
    }

    @Override
    public <T> void store(final @NotNull String key, final @NotNull Codec<T> codec, final @NotNull T value) {
        DataResult<LinTag<?>> result = codec.encodeStart(this.ops, value);
        switch (result) {
            case DataResult.Success<LinTag<?>> success -> this.collector.put(key, PendingTagEntry.ofTag(success.value()));
            case DataResult.Error<LinTag<?>> error -> {
                this.problemReporter.report(() -> "Failed to encode value '" + value + "' to field '" + key + "': " + error.message());
                error.partialValue().ifPresent(tag -> this.collector.put(key, PendingTagEntry.ofTag(tag)));
            }
        }
    }

    @Override
    public <T> void storeNullable(final @NotNull String key, final @NotNull Codec<T> codec, @Nullable final T value) {
        if (value != null) {
            this.store(key, codec, value);
        }
    }

    @Override
    public <T> void store(final @NotNull MapCodec<T> mapCodec, final @NotNull T value) {
        DataResult<LinTag<?>> result = mapCodec.encoder().encodeStart(this.ops, value);
        switch (result) {
            case DataResult.Success<LinTag<?>> success -> {
                if (success.value() instanceof LinCompoundTag compoundTag) {
                    this.merge(compoundTag);
                }
            }
            case DataResult.Error<LinTag<?>> error -> {
                this.problemReporter.report(() -> "Failed to merge value '" + value + "' to an object: " + error.message());
                error.partialValue().filter(tag -> tag instanceof LinCompoundTag)
                        .ifPresent(tag -> merge((LinCompoundTag) tag));
            }
        }
    }

    private void merge(LinCompoundTag other) {
        other.value().forEach((key, tag) -> {
            if (tag instanceof LinCompoundTag compoundTag && this.collector.containsKey(key)) {
                PendingTagEntry entry = this.collector.get(key);
                if (entry.isCompoundBuilder()) {
                    entry.compoundBuilder().put(key, compoundTag);
                    return;
                }
                if (entry.isTag() && entry.tag instanceof LinCompoundTag storedCompound) {
                    this.collector.put(
                            key,
                            PendingTagEntry.ofCompoundBuilder(storedCompound.toBuilder().put(key, compoundTag))
                    );
                    return;
                }
            }
            this.collector.put(key, PendingTagEntry.ofTag(tag));
        });
    }

    @Override
    public void putBoolean(final @NotNull String key, final boolean value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinByteTag.of(value ? (byte) 1 : 0)));
    }

    @Override
    public void putByte(final @NotNull String key, final byte value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinByteTag.of(value)));
    }

    @Override
    public void putShort(final @NotNull String key, final short value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinShortTag.of(value)));
    }

    @Override
    public void putInt(final @NotNull String key, final int value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinIntTag.of(value)));
    }

    @Override
    public void putLong(final @NotNull String key, final long value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinLongTag.of(value)));
    }

    @Override
    public void putFloat(final @NotNull String key, final float value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinFloatTag.of(value)));
    }

    @Override
    public void putDouble(final @NotNull String key, final double value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinDoubleTag.of(value)));
    }

    @Override
    public void putString(final @NotNull String key, final @NotNull String value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinStringTag.of(value)));
    }

    @Override
    public void putIntArray(final @NotNull String key, final int @NotNull [] value) {
        this.collector.put(key, PendingTagEntry.ofTag(LinIntArrayTag.of(value)));
    }

    @Override
    public @NotNull ValueOutput child(final @NotNull String key) {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        this.collector.put(key, PendingTagEntry.ofCompoundBuilder(builder));
        return new LinValueOutput(
                this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)),
                this.ops,
                builder
        );
    }

    @Override
    public @NotNull ValueOutputList childrenList(final @NotNull String key) {
        List<PendingTagEntry> list = new LinkedList<>();
        this.collector.put(key, PendingTagEntry.ofList(list));
        return new ListWrapper(key, this.problemReporter, this.ops, list);
    }

    @Override
    public <T> @NotNull TypedOutputList<T> list(final @NotNull String key, final @NotNull Codec<T> codec) {
        final List<PendingTagEntry> list = new LinkedList<>();
        this.collector.put(key, PendingTagEntry.ofList(list));
        return new TypedListWrapper<>(this.problemReporter, key, this.ops, codec, list);
    }

    @Override
    public void discard(final @NotNull String key) {
        this.collector.remove(key);
    }

    @Override
    public boolean isEmpty() {
        return this.collector.isEmpty();
    }

    public LinCompoundTag buildResult() {
        return this.toBuilder().build();
    }

    public LinCompoundTag.Builder toBuilder() {
        this.collector.forEach((key, value) -> builder.put(key, unwrapPendingEntry(value)));
        return this.builder;
    }

    private LinTag<?> unwrapPendingEntry(final PendingTagEntry entry) {
        if (entry.isTag()) {
            return entry.tag();
        }
        if (entry.isCompoundBuilder()) {
            return entry.compoundBuilder().build();
        }
        if (entry.isList()) {
            return LinOps.rawTagListToLinList(entry.list().stream()
                    .map(this::unwrapPendingEntry).collect(Collectors.toUnmodifiableList()));
        }
        throw new IllegalStateException();
    }

    private record ListWrapper(String fieldName, ProblemReporter problemReporter, DynamicOps<LinTag<?>> ops,
                               List<PendingTagEntry> list) implements ValueOutputList {

        @Override
        public @NotNull ValueOutput addChild() {
            LinCompoundTag.Builder tag = LinCompoundTag.builder();
            this.list.add(PendingTagEntry.ofCompoundBuilder(tag));
            return new LinValueOutput(
                    this.problemReporter.forChild(
                            new ProblemReporter.IndexedFieldPathElement(this.fieldName, this.list.size() - 1)
                    ),
                    this.ops,
                    tag
            );
        }

        @Override
        public void discardLast() {
            this.list.removeLast();
        }

        @Override
        public boolean isEmpty() {
            return this.list.isEmpty();
        }

    }

    private record TypedListWrapper<T>(ProblemReporter problemReporter, String name, DynamicOps<LinTag<?>> ops, Codec<T> codec,
                                       List<PendingTagEntry> list) implements TypedOutputList<T> {

        @Override
        public void add(final T value) {
            DataResult<LinTag<?>> dataResult = this.codec.encodeStart(this.ops, value);
            switch (dataResult) {
                case DataResult.Success<LinTag<?>> success -> this.list.add(PendingTagEntry.ofTag(success.value()));
                case DataResult.Error<LinTag<?>> error -> {
                    this.problemReporter.report(() -> "Failed to append value '" + value + "' to list '" + this.name + "':" + error.message());
                    error.partialValue().map(PendingTagEntry::ofTag).ifPresent(list::add);
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

    }

    private record PendingTagEntry(LinTag<?> tag, List<PendingTagEntry> list, LinCompoundTag.Builder compoundBuilder) {

        public static PendingTagEntry ofTag(LinTag<?> tag) {
            return new PendingTagEntry(tag, null, null);
        }

        public static PendingTagEntry ofList(List<PendingTagEntry> list) {
            return new PendingTagEntry(null, list, null);
        }

        public static PendingTagEntry ofCompoundBuilder(LinCompoundTag.Builder compoundBuilder) {
            return new PendingTagEntry(null, null, compoundBuilder);
        }

        public boolean isTag() {
            return this.tag != null;
        }

        public boolean isList() {
            return this.list != null;
        }

        public boolean isCompoundBuilder() {
            return this.compoundBuilder != null;
        }

    }

}
