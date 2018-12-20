package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class DelegateMCAFilter<T> extends MCAFilter<T> {
    private final MCAFilter<T> filter;

    @Override
    public void withPool(ForkJoinPool pool, MCAQueue queue) {
        filter.withPool(pool, queue);
    }

    @Override
    public boolean appliesFile(Path path, BasicFileAttributes attr) {
        return filter.appliesFile(path, attr);
    }

    @Override
    public boolean appliesFile(int mcaX, int mcaZ) {
        return filter.appliesFile(mcaX, mcaZ);
    }

    @Override
    public MCAFile applyFile(MCAFile file) {
        return filter.applyFile(file);
    }

    @Override
    public boolean appliesChunk(int cx, int cz) {
        return filter.appliesChunk(cx, cz);
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, T cache) {
        return filter.applyChunk(chunk, cache);
    }

    @Override
    public void applyBlock(int x, int y, int z, BaseBlock block, T cache) {
        filter.applyBlock(x, y, z, block, cache);
    }

    @Override
    public void finishChunk(MCAChunk chunk, T cache) {
        filter.finishChunk(chunk, cache);
    }

    @Override
    public void finishFile(MCAFile file, T cache) {
        filter.finishFile(file, cache);
    }

    @Override
    public T init() {
        return filter.init();
    }

    @Override
    public void clean() {
        filter.clean();
    }

    @Override
    public T get() {
        return filter.get();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        filter.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return filter.spliterator();
    }

    @Override
    public void remove() {
        filter.remove();
    }

    @Override
    public void set(T value) {
        filter.set(value);
    }

    public DelegateMCAFilter(MCAFilter<T> filter) {
        this.filter = filter;
    }

    public MCAFilter<T> getFilter() {
        return filter;
    }
}
