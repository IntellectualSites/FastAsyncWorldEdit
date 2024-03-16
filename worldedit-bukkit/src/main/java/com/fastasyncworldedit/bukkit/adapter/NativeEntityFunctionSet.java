package com.fastasyncworldedit.bukkit.adapter;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class NativeEntityFunctionSet<NativeEntity, Result> extends AbstractSet<Result> {

    private final List<NativeEntity> nativeEntities;
    private final Function<NativeEntity, UUID> uuidGetter;
    private final Function<NativeEntity, Result> resultFunction;

    public NativeEntityFunctionSet(
            List<NativeEntity> nativeEntities,
            Function<NativeEntity, UUID> uuidGetter,
            Function<NativeEntity, Result> resultFunction
    ) {
        this.nativeEntities = nativeEntities;
        this.uuidGetter = uuidGetter;
        this.resultFunction = resultFunction;
    }

    @Override
    public int size() {
        return nativeEntities.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object get) {
        if (!(get instanceof com.sk89q.worldedit.entity.Entity e)) {
            return false;
        }
        UUID getUUID = e.getState().getNbtData().getUUID();
        for (NativeEntity entity : nativeEntities) {
            UUID uuid = uuidGetter.apply(entity);
            if (uuid.equals(getUUID)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Iterator<Result> iterator() {
        return nativeEntities
                .stream()
                .map(resultFunction)
                .iterator();
    }

}
