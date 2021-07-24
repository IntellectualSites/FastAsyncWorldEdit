package com.fastasyncworldedit.core.world.block;

import com.sk89q.worldedit.world.block.BlockType;

import java.util.function.Function;

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
