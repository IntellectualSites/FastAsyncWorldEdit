package com.sk89q.worldedit.world.block;

import java.util.function.Predicate;

public class BlockTypeSwitchBuilder<T> {
    private final Object[] runnables;
    private T defaultTask;

    public BlockTypeSwitchBuilder(T defaultTask) {
        this.runnables = new Object[BlockTypes.size()];
        this.defaultTask = defaultTask;
    }

    public BlockTypeSwitchBuilder<T> add(BlockType type, T task) {
        this.runnables[type.getInternalId()] = task;
        return this;
    }

    public BlockTypeSwitchBuilder<T> add(Predicate<BlockType> predicate, T task) {
        for (BlockType type : BlockTypes.values) {
            if (predicate.test(type)) {
                this.runnables[type.getInternalId()] = task;
            }
        }
        return this;
    }

    public BlockTypeSwitchBuilder<T> setDefaultTask(T defaultTask) {
        this.defaultTask = defaultTask;
        return this;
    }

    public BlockTypeSwitch<T> build() {
        for (int i = 0; i < runnables.length; i++) {
            if (runnables[i] == null) runnables[i] = defaultTask;
        }
        return new BlockTypeSwitch(runnables);
    }
}
