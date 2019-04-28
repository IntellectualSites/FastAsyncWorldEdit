package com.boydti.fawe.beta.implementation.blocks;

import com.boydti.fawe.beta.ISetBlocks;
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
