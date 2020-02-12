package com.boydti.fawe.object;

import com.sk89q.worldedit.entity.MapMetadatable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Metadatable implements MapMetadatable {
    private final ConcurrentMap<String, Object> meta = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getRawMeta() {
        return meta;
    }
}
