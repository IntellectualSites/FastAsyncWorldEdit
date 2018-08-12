package com.sk89q.worldedit.world.block;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlockTypeSwitch<T> implements Function<BlockType, T> {
    private final T[] tasks;
    protected BlockTypeSwitch(T[] tasks) {
        this.tasks = tasks;
    }

    @Override
    public T apply(BlockType blockType) {
        return this.tasks[blockType.getInternalId()];
    }
}
