package com.boydti.fawe.bukkit.v1_13.beta;

import com.sk89q.jnbt.CompoundTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class CharSetBlocks extends CharBlocks implements ISetBlocks {
    private byte[] biomes;
    private HashMap<Short, CompoundTag> tiles;
    private HashSet<CompoundTag> entities;
    private HashSet<UUID> entityRemoves;
}
