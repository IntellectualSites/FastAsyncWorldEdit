package com.boydti.fawe.object.changeset;

import com.sk89q.worldedit.history.changeset.ChangeSetSummary;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;

import java.util.HashMap;
import java.util.Map;

public class SimpleChangeSetSummary implements ChangeSetSummary {
    public int[] blocks;

    public int minX;
    public int minZ;

    public int maxX;
    public int maxZ;

    public SimpleChangeSetSummary() {
        blocks = new int[BlockTypesCache.states.length];
        this.minX = Integer.MAX_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxZ = Integer.MIN_VALUE;
    }

    public SimpleChangeSetSummary(int x, int z) {
        blocks = new int[BlockTypesCache.states.length];
        minX = x;
        maxX = x;
        minZ = z;
        maxZ = z;
    }

    public void add(int x, int z, int id) {
        blocks[id]++;
        if (x < minX) {
            minX = x;
        } else if (x > maxX) {
            maxX = x;
        }
        if (z < minZ) {
            minZ = z;
        } else if (z > maxZ) {
            maxZ = z;
        }
    }

    @Override
    public Map<BlockState, Integer> getBlocks() {
        HashMap<BlockState, Integer> map = new HashMap<>();
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] != 0) {
                BlockState state = BlockTypesCache.states[i];
                map.put(state, blocks[i]);
            }
        }
        return map;
    }

    @Override
    public int getSize() {
        int count = 0;
        for (int block : blocks) {
            count += block;
        }
        return count;
    }
}
