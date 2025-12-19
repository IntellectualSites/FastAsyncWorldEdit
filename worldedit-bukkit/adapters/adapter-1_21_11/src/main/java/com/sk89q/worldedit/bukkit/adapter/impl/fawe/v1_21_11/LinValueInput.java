package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_11;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueInput;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinNumberTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class LinValueInput implements ValueInput {

    private final ValueInputContext context;
    private final ProblemReporter problemReporter;
    private final LinCompoundTag input;

    private LinValueInput(ValueInputContext context, ProblemReporter problemReporter, LinCompoundTag input) {
        this.context = context;
        this.problemReporter = problemReporter;
        this.input = input;
    }

    public static LinValueInput create(ProblemReporter problemReporter, HolderLookup.Provider registryAccess, LinCompoundTag input) {
        return new LinValueInput(new ValueInputContext(registryAccess), problemReporter, input);
    }

    @Override
    public <T> @NonNull Optional<T> read(final @NonNull String key, final @NonNull Codec<T> codec) {
        final LinTag<?> tagOrNull = this.input.value().get(key);
        return Optional
                .ofNullable(tagOrNull)
                .map(tag -> codec.parse(this.context.ops(), tag))
                .flatMap(result -> result.resultOrPartial(error -> this.problemReporter.report(() -> "Failed to decode value '" + tagOrNull + "' from field '" + key + "': " + error)));
    }

    @Override
    public <T> @NonNull Optional<T> read(final @NonNull MapCodec<T> mapCodec) {
        return this.context.ops()
                .getMap(this.input)
                .flatMap(mapLike -> mapCodec.decode(this.context.ops(), mapLike))
                .resultOrPartial(error -> this.problemReporter.report(() -> "Failed to decode from map: " + error));
    }

    @Override
    public @NonNull Optional<ValueInput> child(final @NonNull String key) {
        return Optional.ofNullable(this.input.findTag(key, LinTagType.compoundTag())).map(tag -> tag.value().isEmpty()
                ? this.context.empty()
                : new LinValueInput(this.context, this.problemReporter.forChild(new ProblemReporter.FieldPathElement(key)), tag));
    }

    @Override
    public @NonNull ValueInput childOrEmpty(final @NonNull String key) {
        return this.child(key).orElse(this.context.empty());
    }

    @Override
    public @NonNull Optional<ValueInputList> childrenList(final @NonNull String key) {
        //noinspection unchecked
        return Optional
                .ofNullable(this.input.value().get(key))
                .filter(linTag -> linTag instanceof LinListTag<?>)
                .map(tag -> ((LinListTag<LinTag<?>>) tag))
                .map(listTag -> listTag.value().isEmpty()
                        ? this.context.emptyChildList()
                        : new ListWrapper(this.problemReporter, key, listTag, this.context));
    }

    @Override
    public @NonNull ValueInputList childrenListOrEmpty(final @NonNull String key) {
        return this.childrenList(key).orElse(this.context.emptyChildList());
    }

    @Override
    public <T> @NonNull Optional<TypedInputList<T>> list(final @NonNull String key, final @NonNull Codec<T> codec) {
        //noinspection unchecked
        return Optional
                .ofNullable(this.input.value().get(key))
                .filter(linTag -> linTag instanceof LinListTag<?>)
                .map(tag -> ((LinListTag<LinTag<?>>) tag))
                .map(listTag -> listTag.value().isEmpty()
                        ? this.context.emptySafelyTypedList()
                        : new TypedListWrapper<>(this.problemReporter, key, listTag, this.context, codec));
    }

    @Override
    public <T> @NonNull TypedInputList<T> listOrEmpty(final @NonNull String key, final @NonNull Codec<T> codec) {
        return this.list(key, codec).orElse(this.context.emptySafelyTypedList());
    }

    @Override
    public boolean getBooleanOr(final @NonNull String key, final boolean defaultValue) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return numberTag.value().byteValue() != 0;
        }
        return defaultValue;
    }

    @Override
    public byte getByteOr(final @NonNull String key, final byte defaultValue) {
        if (this.input.value().get(key) instanceof LinByteTag byteTag) {
            return byteTag.valueAsByte();
        }
        return defaultValue;
    }

    @Override
    public int getShortOr(final @NonNull String key, final short defaultValue) {
        if (this.input.value().get(key) instanceof LinShortTag shortTag) {
            return shortTag.valueAsShort();
        }
        return defaultValue;
    }

    @Override
    public @NonNull Optional<Integer> getInt(final @NonNull String key) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return Optional.of(numberTag.value().intValue());
        }
        return Optional.empty();
    }

    @Override
    public int getIntOr(final @NonNull String key, final int defaultValue) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return numberTag.value().intValue();
        }
        return defaultValue;
    }

    @Override
    public long getLongOr(final @NonNull String key, final long defaultValue) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return numberTag.value().longValue();
        }
        return defaultValue;
    }

    @Override
    public @NonNull Optional<Long> getLong(final @NonNull String key) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return Optional.of(numberTag.value().longValue());
        }
        return Optional.empty();
    }

    @Override
    public float getFloatOr(final @NonNull String key, final float defaultValue) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return numberTag.value().floatValue();
        }
        return defaultValue;
    }

    @Override
    public double getDoubleOr(final @NonNull String key, final double defaultValue) {
        if (this.input.value().get(key) instanceof LinNumberTag<? extends Number> numberTag) {
            return numberTag.value().doubleValue();
        }
        return defaultValue;
    }

    @Override
    public @NonNull Optional<String> getString(final @NonNull String key) {
        if (this.input.value().get(key) instanceof LinStringTag stringTag) {
            return Optional.of(stringTag.value());
        }
        return Optional.empty();
    }

    @Override
    public @NonNull String getStringOr(final @NonNull String key, final @NonNull String defaultValue) {
        if (this.input.value().get(key) instanceof LinStringTag stringTag) {
            return stringTag.value();
        }
        return defaultValue;
    }

    @Override
    public @NonNull Optional<int[]> getIntArray(final @NonNull String key) {
        if (this.input.value().get(key) instanceof LinIntArrayTag intArrayTag) {
            return Optional.of(intArrayTag.value());
        }
        return Optional.empty();
    }

    @Override
    public HolderLookup.@NonNull Provider lookup() {
        return this.context.lookup();
    }

    private record ValueInputContext(HolderLookup.Provider lookup, DynamicOps<LinTag<?>> ops, ValueInput empty,
                                     ValueInput.ValueInputList emptyChildList, ValueInput.TypedInputList<?> emptyTypedList) {

        ValueInputContext(HolderLookup.Provider lookup) {
            this(
                    lookup,
                    lookup.createSerializationContext(LinOps.INSTANCE),
                    new EmptyValueInput(lookup, new EmptyValueInputList(), new EmptyTypedInputList()),
                    new EmptyValueInputList(),
                    new EmptyTypedInputList()
            );
        }

        public <T> TypedInputList<T> emptySafelyTypedList() {
            //noinspection unchecked
            return (TypedInputList<T>) this.emptyTypedList;
        }

    }

    private record ListWrapper(ProblemReporter problemReporter, String name, LinListTag<LinTag<?>> list,
                               ValueInputContext context) implements ValueInputList {

        @Override
        public boolean isEmpty() {
            return this.list.value().isEmpty();
        }

        @Override
        public @NonNull Stream<ValueInput> stream() {
            return Streams.mapWithIndex(
                    this.list.value().stream(), (tag, index) -> {
                        if (tag instanceof LinCompoundTag compoundTag) {
                            return compoundTag.value().isEmpty() ? this.context.empty() : new LinValueInput(
                                    this.context,
                                    this.problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(
                                            this.name,
                                            (int) index
                                    )),
                                    compoundTag
                            );
                        }
                        this.problemReporter.report(() -> "Expected list '" + this.name + "' to contain at index " + index + " value of type " + LinTagType.compoundTag() + ", but got " + tag.type());
                        return null;
                    }
            ).filter(Objects::nonNull);
        }

        @Override
        public @NonNull Iterator<ValueInput> iterator() {
            final Iterator<LinTag<?>> iterator = this.list.value().iterator();
            return new AbstractIterator<>() {
                private int index;

                @Override
                protected @Nullable ValueInput computeNext() {
                    while (iterator.hasNext()) {
                        LinTag<?> tag = iterator.next();
                        int i = this.index++;
                        if (tag instanceof LinCompoundTag compoundTag) {
                            return compoundTag.value().isEmpty() ? context.empty() : new LinValueInput(
                                    context,
                                    problemReporter.forChild(new ProblemReporter.IndexedFieldPathElement(name, i)),
                                    compoundTag
                            );
                        }
                        problemReporter.report(() -> "Expected list '" + name + "' to contain at index " + index + " value of type " + LinTagType.compoundTag() + ", but got " + tag.type());
                    }
                    return this.endOfData();
                }
            };
        }

    }

    private record TypedListWrapper<T>(ProblemReporter problemReporter, String name, LinListTag<LinTag<?>> list,
                                       ValueInputContext context, Codec<T> codec) implements TypedInputList<T> {

        @Override
        public boolean isEmpty() {
            return this.list.value().isEmpty();
        }

        @Override
        public @NonNull Stream<T> stream() {
            return Streams.mapWithIndex(
                    this.list.value().stream(), (tag, index) -> this.codec
                            .parse(this.context.ops(), tag)
                            .resultOrPartial(error -> this.problemReporter.report(() -> "Failed to decode value '" + tag + "' from field '" + name + "' at index " + index + ": " + error))
                            .orElse(null)
            ).filter(Objects::nonNull);
        }

        @Override
        public @NonNull Iterator<T> iterator() {
            final ListIterator<LinTag<?>> iterator = this.list.value().listIterator();
            return new AbstractIterator<>() {
                @Override
                protected @Nullable T computeNext() {
                    while (true) {
                        if (iterator.hasNext()) {
                            int index = iterator.nextIndex();
                            LinTag<?> tag = iterator.next();
                            switch (codec.parse(context.ops(), tag)) {
                                case DataResult.Success<T> success:
                                    return success.value();
                                case DataResult.Error<T> error:
                                    problemReporter.report(() -> "Failed to decode value '" + tag + "' from field '" + name + "' at index " + index + ":" + " " + error);
                                    if (error.partialValue().isEmpty()) {
                                        continue;
                                    }
                                    return error.partialValue().get();
                            }
                        }
                        return this.endOfData();
                    }
                }
            };
        }

    }

    private static final class EmptyValueInputList implements ValueInput.ValueInputList {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public @NonNull Stream<ValueInput> stream() {
            return Stream.empty();
        }

        @Override
        public @NonNull Iterator<ValueInput> iterator() {
            return Collections.emptyIterator();
        }

    }

    private static final class EmptyTypedInputList implements ValueInput.TypedInputList<Object> {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public @NonNull Stream<Object> stream() {
            return Stream.empty();
        }

        @Override
        public @NonNull Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

    }

    private record EmptyValueInput(HolderLookup.Provider lookup, ValueInputList emptyChildList,
                                   TypedInputList<Object> emptyTypedList) implements ValueInput {

        @Override
        public <T> @NonNull Optional<T> read(final @NonNull String key, final @NonNull Codec<T> codec) {
            return Optional.empty();
        }

        @Override
        public <T> @NonNull Optional<T> read(final @NonNull MapCodec<T> mapCodec) {
            return Optional.empty();
        }

        @Override
        public @NonNull Optional<ValueInput> child(final @NonNull String s) {
            return Optional.empty();
        }

        @Override
        public @NonNull ValueInput childOrEmpty(final @NonNull String s) {
            return EmptyValueInput.this;
        }

        @Override
        public @NonNull Optional<ValueInputList> childrenList(final @NonNull String s) {
            return Optional.empty();
        }

        @Override
        public @NonNull ValueInputList childrenListOrEmpty(final @NonNull String s) {
            return this.emptyChildList;
        }

        @Override
        public <T> @NonNull Optional<TypedInputList<T>> list(final @NonNull String s, final @NonNull Codec<T> codec) {
            return Optional.empty();
        }

        @Override
        public <T> @NonNull TypedInputList<T> listOrEmpty(final @NonNull String s, final @NonNull Codec<T> codec) {
            //noinspection unchecked
            return (TypedInputList<T>) this.emptyTypedList;
        }

        @Override
        public boolean getBooleanOr(final @NonNull String s, final boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public byte getByteOr(final @NonNull String s, final byte defaultValue) {
            return defaultValue;
        }

        @Override
        public int getShortOr(final @NonNull String s, final short defaultValue) {
            return defaultValue;
        }

        @Override
        public @NonNull Optional<Integer> getInt(final @NonNull String s) {
            return Optional.empty();
        }

        @Override
        public int getIntOr(final @NonNull String s, final int defaultValue) {
            return defaultValue;
        }

        @Override
        public long getLongOr(final @NonNull String s, final long defaultValue) {
            return defaultValue;
        }

        @Override
        public @NonNull Optional<Long> getLong(final @NonNull String s) {
            return Optional.empty();
        }

        @Override
        public float getFloatOr(final @NonNull String s, final float defaultValue) {
            return defaultValue;
        }

        @Override
        public double getDoubleOr(final @NonNull String s, final double defaultValue) {
            return defaultValue;
        }

        @Override
        public @NonNull Optional<String> getString(final @NonNull String s) {
            return Optional.empty();
        }

        @Override
        public @NonNull String getStringOr(final @NonNull String s, final @NonNull String defaultValue) {
            return defaultValue;
        }

        @Override
        public @NonNull Optional<int[]> getIntArray(final @NonNull String s) {
            return Optional.empty();
        }

    }

}
