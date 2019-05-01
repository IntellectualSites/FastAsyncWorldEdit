package com.sk89q.worldedit.entity;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class LazyBaseEntity extends BaseEntity {
    private Supplier<CompoundTag> saveTag;
    public LazyBaseEntity(EntityType type, Supplier<CompoundTag> saveTag) {
        super(type, null);
        this.saveTag = saveTag;
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        Supplier<CompoundTag> tmp = saveTag;
        if (tmp != null) {
            saveTag = null;
            if (Fawe.isMainThread()) {
                setNbtData(tmp.get());
            } else {
                setNbtData(TaskManager.IMP.sync(tmp));
            }
        }
        return super.getNbtData();
    }
}
