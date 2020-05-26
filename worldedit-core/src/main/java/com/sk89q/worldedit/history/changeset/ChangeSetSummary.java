package com.sk89q.worldedit.history.changeset;

import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ChangeSetSummary {
    Map<BlockState, Integer> getBlocks();

    int getSize();

    default List<Countable<BlockState>> getBlockDistributionWithData() {
        ArrayList<Countable<BlockState>> list = new ArrayList<>();
        for (Map.Entry<BlockState, Integer> entry : getBlocks().entrySet()) {
            list.add(new Countable<>(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    default Map<BlockState, Double> getPercents() {
        Map<BlockState, Integer> map = getBlocks();
        int count = getSize();
        Map<BlockState, Double> newMap = new HashMap<>();
        for (Map.Entry<BlockState, Integer> entry : map.entrySet()) {
            BlockState id = entry.getKey();
            int changes = entry.getValue();
            double percent = (changes * 1000L / count) / 10d;
            newMap.put(id, percent);
        }
        return newMap;
    }
}
