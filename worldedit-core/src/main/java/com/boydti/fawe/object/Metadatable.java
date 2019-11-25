package com.boydti.fawe.object;

import com.sk89q.worldedit.entity.MapMetadatable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metadatable implements MapMetadatable {
    private final ConcurrentHashMap<String, Object> meta = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getRawMeta() {
        return meta;
    }
}
