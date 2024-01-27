package com.fastasyncworldedit.core.entity;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class LazyBaseEntity extends BaseEntity {

    private Supplier<CompoundBinaryTag> saveTag;

    public LazyBaseEntity(EntityType type, Supplier<CompoundBinaryTag> saveTag) {
        super(type);
        this.saveTag = saveTag;
    }

    @Nullable
    @Override
    public CompoundBinaryTag getNbt() {
        Supplier<CompoundBinaryTag> tmp = saveTag;
        if (tmp != null) {
            saveTag = null;
            if (Fawe.isMainThread()) {
                setNbt(tmp.get());
            } else {
                // TODO (folia)
                // setNbt(TaskManager.taskManager().sync(tmp));
            }
        }
        return super.getNbt();
    }

}
