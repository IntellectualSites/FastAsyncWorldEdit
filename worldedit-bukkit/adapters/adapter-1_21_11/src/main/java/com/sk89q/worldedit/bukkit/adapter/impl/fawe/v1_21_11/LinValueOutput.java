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
import org.jetbrains.annotations.VisibleForTesting;

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
    private final Map<String, PendingEntry> collector;

    LinValueOutput(final ProblemReporter reporter, final DynamicOps<LinTag<?>> ops) {
        this.problemReporter = reporter;
        this.ops = ops;
        this.collector = new LinkedHashMap<>();
    }

    public static LinValueOutput createWithContext(ProblemReporter reporter, HolderLookup.Provider lookup) {
        return new LinValueOutput(reporter, lookup.createSerializationContext(LinOps.INSTANCE));
    }

    @Override
    public <T> void store(final @NotNull String key, final @NotNull Codec<T> codec, final @NotNull T value) {
        DataResult<LinTag<?>> result = codec.encodeStart(this.ops, value);
        switch (result) {
            case DataResult.Success<LinTag<?>> success -> this.collector.put(key, PendingEntry.ofTag(success.value()));
            case DataResult.Error<LinTag<?>> error -> {
                this.problemReporter.report(() -> "Failed to encode value '" + value + "' to field '" + key + "': " + error.message());
                error.partialValue().ifPresent(tag -> this.collector.put(key, PendingEntry.ofTag(tag)));
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

    @VisibleForTesting
    void merge(LinCompoundTag other) {
        other.value().forEach((key, tag) -> {
            if (tag instanceof LinCompoundTag compoundTag && this.collector.containsKey(key)) {
                PendingEntry entry = this.collector.get(key);
                if (entry instanceof PendingTagEntry(LinTag<?> value) && value instanceof LinCompoundTag storedCompound) {
                    this.collector.put(key, PendingEntry.ofTag(merge(compoundTag, storedCompound)));
                    return;
                }
                if (entry instanceof PendingLinValueOutputEntry(LinValueOutput valueOutput)) {
                    this.collector.put(key, PendingEntry.ofTag(merge(compoundTag, valueOutput.buildResult())));
                    return;
                }
            }
            this.collector.put(key, PendingEntry.ofTag(tag));
        });
    }

    private static LinCompoundTag merge(LinCompoundTag source, LinCompoundTag target) {
        LinCompoundTag.Builder builder = target.toBuilder();
        source.value().forEach((key, tag) -> {
            if (target.value().get(key) instanceof LinCompoundTag targetCompoundTag && tag instanceof LinCompoundTag sourceCompoundTag) {
                builder.put(key, merge(sourceCompoundTag, targetCompoundTag));
                return;
            }
            builder.put(key, tag);
        });
        return builder.build();
    }

    @Override
    public void putBoolean(final @NotNull String key, final boolean value) {
        this.collector.put(key, PendingEntry.ofTag(LinByteTag.of(value ? (byte) 1 : 0)));
    }

    @Override
    public void putByte(final @NotNull String key, final byte value) {
        this.collector.put(key, PendingEntry.ofTag(LinByteTag.of(value)));
    }

    @Override
    public void putShort(final @NotNull String key, final short value) {
        this.collector.put(key, PendingEntry.ofTag(LinShortTag.of(value)));
    }

    @Override
    public void putInt(final @NotNull String key, final int value) {
        this.collector.put(key, PendingEntry.ofTag(LinIntTag.of(value)));
    }

    @Override
    public void putLong(final @NotNull String key, final long value) {
        this.collector.put(key, PendingEntry.ofTag(LinLongTag.of(value)));
    }

    @Override
    public void putFloat(final @NotNull String key, final float value) {
        this.collector.put(key, PendingEntry.ofTag(LinFloatTag.of(value)));
    }

    @Override
    public void putDouble(final @NotNull String key, final double value) {
        this.collector.put(key, PendingEntry.ofTag(LinDoubleTag.of(value)));
    }

    @Override
    public void putString(final @NotNull String key, final @NotNull String value) {
        this.collector.put(key, PendingEntry.ofTag(LinStringTag.of(value)));
    }

    @Override
    public void putIntArray(final @NotNull String key, final int @NotNull [] value) {
        this.collector.put(key, PendingEntry.ofTag(LinIntArrayTag.of(value)));
    }

    @Override
    public @NotNull ValueOutput child(final @NotNull String key) {
        LinValueOutput output = new LinValueOutput(
                this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)),
                this.ops
        );
        this.collector.put(key, PendingEntry.ofLinValueOutputEntry(output));
        return output;
    }

    @Override
    public @NotNull ValueOutputList childrenList(final @NotNull String key) {
        List<PendingEntry> list = new LinkedList<>();
        this.collector.put(key, PendingEntry.ofList(list));
        return new ListWrapper(key, this.problemReporter, this.ops, list);
    }

    @Override
    public <T> @NotNull TypedOutputList<T> list(final @NotNull String key, final @NotNull Codec<T> codec) {
        final List<PendingEntry> list = new LinkedList<>();
        this.collector.put(key, PendingEntry.ofList(list));
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
        return LinCompoundTag.of(this.collector.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> unwrapPendingEntry(entry.getValue())))
        );
    }

    public LinCompoundTag.Builder toBuilder() {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        this.collector.forEach((key, value) -> builder.put(key, unwrapPendingEntry(value)));
        return builder;
    }

    private static LinTag<?> unwrapPendingEntry(final PendingEntry entry) {
        return switch (entry) {
            case PendingTagEntry e -> e.tag();
            case PendingLinValueOutputEntry e -> e.valueOutput().buildResult();
            case PendingListEntry e -> LinOps.rawTagListToLinList(
                    e.entries().stream().map(LinValueOutput::unwrapPendingEntry).collect(Collectors.toList())
            );
        };
    }

    private record ListWrapper(String fieldName, ProblemReporter problemReporter, DynamicOps<LinTag<?>> ops,
                               List<PendingEntry> list) implements ValueOutputList {

        @Override
        public @NotNull ValueOutput addChild() {
            LinValueOutput valueOutput = new LinValueOutput(
                    this.problemReporter.forChild(
                            new ProblemReporter.IndexedFieldPathElement(this.fieldName, this.list.size() - 1)
                    ),
                    this.ops
            );
            this.list.add(PendingEntry.ofLinValueOutputEntry(valueOutput));
            return valueOutput;
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
                                       List<PendingEntry> list) implements TypedOutputList<T> {

        @Override
        public void add(final T value) {
            DataResult<LinTag<?>> dataResult = this.codec.encodeStart(this.ops, value);
            switch (dataResult) {
                case DataResult.Success<LinTag<?>> success -> this.list.add(PendingEntry.ofTag(success.value()));
                case DataResult.Error<LinTag<?>> error -> {
                    this.problemReporter.report(() -> "Failed to append value '" + value + "' to list '" + this.name + "':" + error.message());
                    error.partialValue().map(PendingEntry::ofTag).ifPresent(list::add);
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

    }

    private sealed interface PendingEntry permits PendingTagEntry, PendingListEntry, PendingLinValueOutputEntry {

        static PendingEntry ofTag(LinTag<?> tag) {
            return new PendingTagEntry(tag);
        }

        static PendingEntry ofList(List<PendingEntry> list) {
            return new PendingListEntry(list);
        }

        static PendingEntry ofLinValueOutputEntry(LinValueOutput valueOutput) {
            return new PendingLinValueOutputEntry(valueOutput);
        }

    }

    private record PendingTagEntry(LinTag<?> tag) implements PendingEntry {

    }

    private record PendingListEntry(List<PendingEntry> entries) implements PendingEntry {

    }

    private record PendingLinValueOutputEntry(LinValueOutput valueOutput) implements PendingEntry {

    }

}
