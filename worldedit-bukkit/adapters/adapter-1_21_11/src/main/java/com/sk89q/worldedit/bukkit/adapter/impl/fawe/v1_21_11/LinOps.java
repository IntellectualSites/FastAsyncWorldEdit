package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_11;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.enginehub.linbus.common.LinTagId;
import org.enginehub.linbus.tree.LinByteArrayTag;
import org.enginehub.linbus.tree.LinByteTag;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinEndTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinLongArrayTag;
import org.enginehub.linbus.tree.LinLongTag;
import org.enginehub.linbus.tree.LinNumberTag;
import org.enginehub.linbus.tree.LinShortTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DynamicOps implementation for direct LinBus interaction.
 * Basically a copy of {@link net.minecraft.nbt.NbtOps}, but for Lin (with changes so it actually works).
 */
public class LinOps implements DynamicOps<LinTag<?>> {

    static final DynamicOps<LinTag<?>> INSTANCE = new LinOps();

    private LinOps() {
    }

    @Override
    public LinTag<?> empty() {
        return LinEndTag.instance();
    }

    @Override
    public LinTag<?> emptyList() {
        return LinListTag.empty(LinTagType.endTag());
    }

    @Override
    public LinTag<?> emptyMap() {
        return LinCompoundTag.builder().build();
    }

    @Override
    public <U> U convertTo(final DynamicOps<U> outOps, final LinTag<?> input) {
        return switch (input) {
            case LinEndTag ignored -> outOps.empty();
            case LinByteTag tag -> outOps.createByte(tag.valueAsByte());
            case LinShortTag tag -> outOps.createShort(tag.valueAsShort());
            case LinIntTag tag -> outOps.createInt(tag.valueAsInt());
            case LinLongTag tag -> outOps.createLong(tag.valueAsLong());
            case LinFloatTag tag -> outOps.createFloat(tag.valueAsFloat());
            case LinDoubleTag tag -> outOps.createDouble(tag.valueAsDouble());
            case LinByteArrayTag tag -> outOps.createByteList(ByteBuffer.wrap(tag.value()));
            case LinStringTag tag -> outOps.createString(tag.value());
            case LinListTag<?> tag -> convertList(outOps, tag);
            case LinCompoundTag tag -> convertMap(outOps, tag);
            case LinIntArrayTag tag -> outOps.createIntList(Arrays.stream(tag.value()));
            case LinLongArrayTag tag -> outOps.createLongList(Arrays.stream(tag.value()));
        };
    }

    @Override
    public DataResult<Number> getNumberValue(final LinTag<?> input) {
        if (input instanceof LinNumberTag<? extends Number> tag) {
            return DataResult.success(tag.value());
        }
        return DataResult.error(() -> "Not a number");
    }

    @Override
    public LinTag<?> createNumeric(final Number i) {
        return LinDoubleTag.of(i.doubleValue());
    }

    @Override
    public LinTag<?> createByte(final byte value) {
        return LinByteTag.of(value);
    }

    @Override
    public LinTag<?> createShort(final short value) {
        return LinShortTag.of(value);
    }

    @Override
    public LinTag<?> createInt(final int value) {
        return LinIntTag.of(value);
    }

    @Override
    public LinTag<?> createLong(final long value) {
        return LinLongTag.of(value);
    }

    @Override
    public LinTag<?> createFloat(final float value) {
        return LinFloatTag.of(value);
    }

    @Override
    public LinTag<?> createDouble(final double value) {
        return LinDoubleTag.of(value);
    }

    @Override
    public LinTag<?> createBoolean(final boolean value) {
        return LinByteTag.of(value ? (byte) 1 : (byte) 0);
    }

    @Override
    public DataResult<String> getStringValue(final LinTag<?> input) {
        if (input instanceof LinStringTag tag) {
            return DataResult.success(tag.value());
        }
        return DataResult.error(() -> "Not a string");
    }

    @Override
    public LinTag<?> createString(final String value) {
        return LinStringTag.of(value);
    }

    @Override
    public DataResult<LinTag<?>> mergeToList(final LinTag<?> list, final LinTag<?> value) {
        return createCollector(list)
                .<DataResult<LinTag<?>>>map(collector -> DataResult.success(collector.accept(value).result()))
                .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
    }

    @Override
    public DataResult<LinTag<?>> mergeToList(final LinTag<?> list, final List<LinTag<?>> values) {
        return createCollector(list)
                .<DataResult<LinTag<?>>>map(collector -> DataResult.success(collector.acceptAll(values).result()))
                .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + list, list));
    }

    @Override
    public DataResult<LinTag<?>> mergeToMap(final LinTag<?> map, final LinTag<?> key, final LinTag<?> value) {
        if (!(map instanceof LinCompoundTag) && !(map instanceof LinEndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        }
        if (!(key instanceof LinStringTag keyStringTag)) {
            return DataResult.error(() -> "key is not a string: " + key, map);
        }
        if (map instanceof LinCompoundTag mapCompoundTag) {
            return DataResult.success(mapCompoundTag.toBuilder().put(keyStringTag.value(), value).build());
        }
        return DataResult.success(LinCompoundTag.builder().put(keyStringTag.value(), value).build());
    }

    @Override
    public DataResult<Stream<Pair<LinTag<?>, LinTag<?>>>> getMapValues(final LinTag<?> input) {
        if (input instanceof LinCompoundTag tag) {
            Map<String, LinTag<?>> value = tag.value();
            return DataResult.success(value
                    .entrySet()
                    .stream()
                    .map(entry -> Pair.of(this.createString(entry.getKey()), entry.getValue())));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<LinTag<?>, LinTag<?>>>> getMapEntries(final LinTag<?> input) {
        if (input instanceof LinCompoundTag tag) {
            return DataResult.success(consumer ->
                    tag.value().forEach((s, linTag) -> consumer.accept(this.createString(s), linTag)));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<MapLike<LinTag<?>>> getMap(final LinTag<?> input) {
        if (input instanceof LinCompoundTag map) {
            Map<String, LinTag<?>> value = map.value();
            return DataResult.success(new MapLike<>() {
                @Override
                public @Nullable LinTag<?> get(final LinTag<?> key) {
                    if (key instanceof LinStringTag tag) {
                        return value.get(tag.value());
                    }
                    throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + key);
                }

                @Override
                public @Nullable LinTag<?> get(final String key) {
                    return value.get(key);
                }

                @Override
                public Stream<Pair<LinTag<?>, LinTag<?>>> entries() {
                    return value.entrySet().stream().map(entry -> Pair.of(
                            LinOps.this.createString(entry.getKey()),
                            entry.getValue()
                    ));
                }

                @Override
                public String toString() {
                    return "MapLike[" + map + "]";
                }
            });
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public LinTag<?> createMap(final Stream<Pair<LinTag<?>, LinTag<?>>> map) {
        LinCompoundTag.Builder builder = LinCompoundTag.builder();
        map.forEach(pair -> {
            if (pair.getFirst() instanceof LinStringTag key) {
                builder.put(key.value(), pair.getSecond());
                return;
            }
            throw new UnsupportedOperationException("Cannot create map entry with non-string key: " + pair.getFirst());
        });
        return builder.build();
    }

    @Override
    public DataResult<Stream<LinTag<?>>> getStream(final LinTag<?> input) {
        return switch (input) {
            case LinListTag<?> tag -> DataResult.success(StreamSupport.stream(
                    Spliterators.spliterator(
                            new Iterator<>() {
                                private int index;

                                @Override
                                public boolean hasNext() {
                                    return this.index < tag.value().size();
                                }

                                @Override
                                public LinTag<?> next() {
                                    if (!this.hasNext()) {
                                        throw new NoSuchElementException();
                                    }
                                    return tag.get(index++);
                                }
                            }, tag.value().size(), 0
                    ), false
            ));
            case LinByteArrayTag tag -> DataResult.success(StreamSupport.stream(
                    Spliterators.spliterator(
                            new Iterator<LinByteTag>() {
                                private int index;

                                @Override
                                public boolean hasNext() {
                                    return this.index < tag.value().length;
                                }

                                @Override
                                public LinByteTag next() {
                                    if (!this.hasNext()) {
                                        throw new NoSuchElementException();
                                    }
                                    return LinByteTag.of(tag.value()[index++]);
                                }
                            }, tag.value().length, 0
                    ), false
            ));
            case LinIntArrayTag tag -> DataResult.success(
                    StreamSupport.stream(Spliterators.spliterator(tag.value(), 0), false)
                            .map(LinIntTag::of)
            );
            case LinLongArrayTag tag -> DataResult.success(
                    StreamSupport.stream(Spliterators.spliterator(tag.value(), 0), false)
                            .map(LinLongTag::of)
            );
            default -> DataResult.error(() -> "Not a list");
        };
    }

    @Override
    public DataResult<Consumer<Consumer<LinTag<?>>>> getList(final LinTag<?> input) {
        if (input instanceof LinListTag<?> tag) {
            return DataResult.success(consumer -> tag.value().forEach(consumer));
        }
        if (input instanceof LinByteArrayTag tag) {
            return DataResult.success(consumer -> {
                for (final byte b : tag.value()) {
                    consumer.accept(this.createByte(b));
                }
            });
        }
        if (input instanceof LinIntArrayTag tag) {
            return DataResult.success(consumer -> {
                for (final int i : tag.value()) {
                    consumer.accept(this.createInt(i));
                }
            });
        }
        if (input instanceof LinLongArrayTag tag) {
            return DataResult.success(consumer -> {
                for (final long l : tag.value()) {
                    consumer.accept(this.createLong(l));
                }
            });
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(final LinTag<?> input) {
        if (input instanceof LinByteArrayTag tag) {
            return DataResult.success(ByteBuffer.wrap(tag.value()));
        }
        return DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public LinTag<?> createByteList(final ByteBuffer input) {
        ByteBuffer buffer = input.duplicate().clear();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(0, bytes, 0, bytes.length);
        return LinByteArrayTag.of(bytes);
    }

    @Override
    public DataResult<IntStream> getIntStream(final LinTag<?> input) {
        if (input instanceof LinIntArrayTag tag) {
            return DataResult.success(IntStream.of(tag.value()));
        }
        return DynamicOps.super.getIntStream(input);
    }

    @Override
    public LinTag<?> createIntList(final IntStream input) {
        return LinIntArrayTag.of(input.toArray());
    }

    @Override
    public DataResult<LongStream> getLongStream(final LinTag<?> input) {
        if (input instanceof LinLongArrayTag tag) {
            return DataResult.success(LongStream.of(tag.value()));
        }
        return DynamicOps.super.getLongStream(input);
    }

    @Override
    public LinTag<?> createLongList(final LongStream input) {
        return LinLongArrayTag.of(input.toArray());
    }

    @Override
    public LinTag<?> createList(final Stream<LinTag<?>> input) {
        return rawTagListToLinList(input.collect(Collectors.toList()));
    }

    @Override
    public LinTag<?> remove(final LinTag<?> input, final String key) {
        if (input instanceof LinCompoundTag tag) {
            return tag.toBuilder().remove(key).build();
        }
        return input;
    }

    @Override
    public String toString() {
        return "LinOps";
    }

    @Override
    public RecordBuilder<LinTag<?>> mapBuilder() {
        return new LinRecordBuilder();
    }

    private static Optional<ListCollector> createCollector(LinTag<?> tag) {
        return switch (tag) {
            case LinEndTag ignored -> Optional.of(new GenericListCollector());
            case LinListTag<?> listTag -> Optional.of(new GenericListCollector(listTag));
            case LinByteArrayTag byteArrayTag -> Optional.of(new ByteListCollector(byteArrayTag));
            case LinIntArrayTag intArrayTag -> Optional.of(new IntListCollector(intArrayTag));
            case LinLongArrayTag longArrayTag -> Optional.of(new LongListCollector(longArrayTag));
            default -> Optional.empty();
        };
    }

    // net.minecraft.nbt.ListTag#identifyRawElementType
    private static LinTagId identifyTagTypeOfUncheckedList(List<LinTag<?>> list) {
        LinTagId type = LinTagId.END;
        for (final LinTag<?> linTag : list) {
            if (type == LinTagId.END) {
                type = linTag.type().id();
                continue;
            }
            if (type != linTag.type().id()) {
                return LinTagId.COMPOUND;
            }
        }
        return type;
    }

    static LinTag<?> rawTagListToLinList(List<LinTag<?>> content) {
        final LinTagId typeId = identifyTagTypeOfUncheckedList(content);
        LinListTag.Builder<LinTag<?>> builder = LinListTag.builder(LinTagType.fromId(typeId));
        for (final LinTag<?> entry : content) {
            if (typeId == LinTagId.COMPOUND && !(entry instanceof LinCompoundTag)) {
                builder.add(LinCompoundTag.of(Map.of("", entry)));
                continue;
            }
            builder.add(entry);
        }
        return builder.build();
    }

    private interface ListCollector {

        ListCollector accept(LinTag<?> tag);

        default ListCollector acceptAll(Iterable<LinTag<?>> iterable) {
            ListCollector collector = this;
            for (final LinTag<?> linTag : iterable) {
                collector = collector.accept(linTag);
            }
            return collector;
        }

        LinTag<?> result();

    }

    private static class GenericListCollector implements ListCollector {

        private final List<LinTag<?>> list = new ArrayList<>();

        GenericListCollector() {
        }

        GenericListCollector(LinListTag<?> listTag) {
            this.list.addAll(listTag.value());
        }

        GenericListCollector(ByteList byteList) {
            byteList.forEach(value -> list.add(LinByteTag.of(value)));
        }

        GenericListCollector(IntList intList) {
            intList.forEach(value -> list.add(LinIntTag.of(value)));
        }

        GenericListCollector(LongList longList) {
            longList.forEach(value -> list.add(LinLongTag.of(value)));
        }

        @Override
        public ListCollector accept(final LinTag<?> tag) {
            this.list.add(tag);
            return this;
        }

        @Override
        public LinTag<?> result() {
            return rawTagListToLinList(this.list);
        }

    }

    private record ByteListCollector(ByteList byteList) implements ListCollector {

        private ByteListCollector(final LinByteArrayTag byteList) {
            this(ByteArrayList.of(byteList.value()));
        }

        @Override
        public ListCollector accept(final LinTag<?> tag) {
            if (tag instanceof LinByteTag byteTag) {
                byteList.add(byteTag.valueAsByte());
                return this;
            }
            return new GenericListCollector(this.byteList).accept(tag);
        }

        @Override
        public LinTag<?> result() {
            return LinByteArrayTag.of(this.byteList.toByteArray());
        }

    }

    private record IntListCollector(IntList intList) implements ListCollector {

        private IntListCollector(final LinIntArrayTag intList) {
            this(IntArrayList.of(intList.value()));
        }

        @Override
        public ListCollector accept(final LinTag<?> tag) {
            if (tag instanceof LinIntTag intTag) {
                intList.add(intTag.valueAsInt());
                return this;
            }
            return new GenericListCollector(this.intList).accept(tag);
        }

        @Override
        public LinTag<?> result() {
            return LinIntArrayTag.of(this.intList.toIntArray());
        }

    }

    private record LongListCollector(LongList longList) implements ListCollector {

        private LongListCollector(final LinLongArrayTag longList) {
            this(LongArrayList.of(longList.value()));
        }

        @Override
        public ListCollector accept(final LinTag<?> tag) {
            if (tag instanceof LinLongTag longTag) {
                longList.add(longTag.valueAsLong());
                return this;
            }
            return new GenericListCollector(this.longList).accept(tag);
        }

        @Override
        public LinTag<?> result() {
            return LinLongArrayTag.of(this.longList.toLongArray());
        }

    }

    private class LinRecordBuilder extends RecordBuilder.AbstractStringBuilder<LinTag<?>, LinCompoundTag.Builder> {

        protected LinRecordBuilder() {
            super(LinOps.this);
        }

        @Override
        protected LinCompoundTag.Builder initBuilder() {
            return LinCompoundTag.builder();
        }

        @Override
        protected LinCompoundTag.Builder append(final String key, final LinTag<?> value, final LinCompoundTag.Builder builder) {
            return builder.put(key, value);
        }

        @Override
        protected DataResult<LinTag<?>> build(final LinCompoundTag.Builder builder, final LinTag<?> prefix) {
            if (prefix != null && !prefix.type().equals(LinTagType.endTag())) {
                if (!(prefix instanceof LinCompoundTag prefixCompound)) {
                    return DataResult.error(() -> "mergeToMap called with not a map: " + prefix, prefix);
                }
                LinCompoundTag.Builder prefixedBuilder = prefixCompound.toBuilder();
                builder.build().value().forEach(prefixedBuilder::put);
                return DataResult.success(prefixedBuilder.build());
            }
            return DataResult.success(builder.build());
        }

    }

}
