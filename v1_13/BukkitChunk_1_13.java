package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.adapter.v1_13_1.BlockMaterial_1_13;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Blocks;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.DataPaletteGlobal;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.TileEntity;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BukkitChunk_1_13 extends IntFaweChunk<Chunk, BukkitQueue_1_13> {

    public DataPaletteBlock[] sectionPalettes;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_1_13(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public BukkitChunk_1_13(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    public void storeBiomes(BiomeType[] biomes) {
        this.biomes = Arrays.copyOf(biomes, biomes.length);
    }

    public boolean storeTile(TileEntity tile, BlockPosition pos) {
        NBTTagCompound tag = new NBTTagCompound();
        CompoundTag nativeTag = getParent().getTag(tile);
        setTile(pos.getX() & 15, pos.getY(), pos.getZ() & 15, nativeTag);
        return true;
    }

    public boolean storeEntity(Entity ent) throws InvocationTargetException, IllegalAccessException {
        if (ent instanceof EntityPlayer || BukkitQueue_0.getAdapter() == null) {
            return false;
        }
        int x = (MathMan.roundInt(ent.locX) & 15);
        int z = (MathMan.roundInt(ent.locZ) & 15);
        int y = (MathMan.roundInt(ent.locY) & 0xFF);
        int i = FaweCache.CACHE_I[y][z][x];
        int j = FaweCache.CACHE_J[y][z][x];
        EntityTypes<?> type = ent.P();
        MinecraftKey id = EntityTypes.getName(type);
        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            ent.save(tag); // readEntityIntoTag
            CompoundTag nativeTag = (CompoundTag) BukkitQueue_0.toNative(tag);
            Map<String, Tag> map = ReflectionUtils.getMap(nativeTag.getValue());
            map.put("Id", new StringTag(id.toString()));
            setEntity(nativeTag);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IntFaweChunk<Chunk, BukkitQueue_1_13> copy(boolean shallow) {
        BukkitChunk_1_13 copy;
        if (shallow) {
            copy = new BukkitChunk_1_13(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_13(getParent(), getX(), getZ(), (int[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
            copy.chunk = chunk;
        }
        if (sectionPalettes != null) {
            copy.sectionPalettes = new DataPaletteBlock[16];
            try {
                for (int i = 0; i < sectionPalettes.length; i++) {
                    DataPaletteBlock current = sectionPalettes[i];
                    if (current == null) {
                        continue;
                    }
                    // Clone palette
                    DataPalette currentPalette = (DataPalette) BukkitQueue_1_13.fieldPalette.get(current);
                    if (!(currentPalette instanceof DataPaletteGlobal)) {
                        // TODO support non global palette
                        BukkitQueue_1_13.methodResize.invoke(current, 128);
                        currentPalette = (DataPalette) BukkitQueue_1_13.fieldPalette.get(current);
                        if (!(currentPalette instanceof DataPaletteGlobal)) {
                            throw new RuntimeException("Palette must be global!");
                        }
                    }
                    DataPaletteBlock<IBlockData> paletteBlock = newDataPaletteBlock();
                    BukkitQueue_1_13.fieldPalette.set(paletteBlock, currentPalette);
                    // Clone size
                    BukkitQueue_1_13.fieldSize.set(paletteBlock, BukkitQueue_1_13.fieldSize.get(current));
                    // Clone palette
                    DataBits currentBits = (DataBits) BukkitQueue_1_13.fieldBits.get(current);
                    DataBits newBits = new DataBits(1, 0);
                    for (Field field : DataBits.class.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object currentValue = field.get(currentBits);
                        if (currentValue instanceof long[]) {
                            currentValue = ((long[]) currentValue).clone();
                        }
                        field.set(newBits, currentValue);
                    }
                    BukkitQueue_1_13.fieldBits.set(paletteBlock, newBits);
                    copy.sectionPalettes[i] = paletteBlock;
                }
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        return copy;
    }

    private DataPaletteBlock<IBlockData> newDataPaletteBlock() {
        return new DataPaletteBlock<>(ChunkSection.GLOBAL_PALETTE, Block.REGISTRY_ID, GameProfileSerializer::d, GameProfileSerializer::a, Blocks.AIR.getBlockData());
    }

    @Override
    public Chunk getNewChunk() {
        return ((BukkitQueue_1_13) getParent()).getWorld().getChunkAt(getX(), getZ());
    }

    public void optimize() {
        if (sectionPalettes != null) {
            return;
        }
        int[][] arrays = getCombinedIdArrays();
        char lastChar = Character.MAX_VALUE;
        for (int layer = 0; layer < 16; layer++) {
            if (getCount(layer) > 0) {
                if (sectionPalettes == null) {
                    sectionPalettes = new DataPaletteBlock[16];
                }
                DataPaletteBlock palette = newDataPaletteBlock();
                int[] blocks = getIdArray(layer);
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int combinedId = blocks[FaweCache.CACHE_J[y][z][x]];
                            if (combinedId != 0) {
                                BlockType state = BlockTypes.getFromStateId(combinedId);
                                IBlockData blockData = ((BlockMaterial_1_13) state.getMaterial()).getState();
                                palette.setBlock(x, y, z, blockData);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    private void removeEntity(Entity entity) {
        entity.b(false);
        entity.die();
        entity.valid = false;
    }

    @Override
    public FaweChunk call() {
        try {
            BukkitChunk_1_13_Copy copy = getParent().getChangeTask() != null ? new BukkitChunk_1_13_Copy(getParent(), getX(), getZ()) : null;
            final Chunk chunk = this.getChunk();
            final World world = chunk.getWorld();
            Settings settings = getParent().getSettings();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;
            final boolean flag = world.getEnvironment() == World.Environment.NORMAL;
            net.minecraft.server.v1_13_R2.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            nmsChunk.f(true); // Set Modified
            nmsChunk.mustSave = true;
            net.minecraft.server.v1_13_R2.World nmsWorld = nmsChunk.world;
            ChunkSection[] sections = nmsChunk.getSections();
            List<Entity>[] entities = nmsChunk.getEntitySlices();
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            // Set heightmap
            getParent().setHeightMap(this, heightMap);
            // Remove entities
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                for (int i = 0; i < entities.length; i++) {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        Iterator<Entity> iter = ents.iterator();
                        while (iter.hasNext()) {
                            Entity entity = iter.next();
                            if (entsToRemove.contains(entity.getUniqueID())) {
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                iter.remove();
                                synchronized (BukkitQueue_0.class) {
                                    removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < entities.length; i++) {
                int count = this.getCount(i);
                if (count == 0 || settings.EXPERIMENTAL.KEEP_ENTITIES_IN_BLOCKS) {
                    continue;
                } else if (count >= 4096) {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        synchronized (BukkitQueue_0.class) {
                            Iterator<Entity> iter = ents.iterator();
                            while (iter.hasNext()) {
                                Entity entity = iter.next();
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                iter.remove();
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                removeEntity(entity);
                            }
                        }
                    }
                } else {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        int layerYStart = i << 4;
                        int layerYEnd = layerYStart + 15;
                        int[] array = this.getIdArray(i);
                        if (array == null) continue;
                        Iterator<Entity> iter = ents.iterator();
                        while (iter.hasNext()) {
                            Entity entity = iter.next();
                            if (entity instanceof EntityPlayer) {
                                continue;
                            }
                            int y = MathMan.roundInt(entity.locY);
                            if (y > layerYEnd || y < layerYStart) continue;
                            int x = (MathMan.roundInt(entity.locX) & 15);
                            int z = (MathMan.roundInt(entity.locZ) & 15);
                            if (array[FaweCache.CACHE_J[y][z][x]] != 0) {
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                iter.remove();
                                synchronized (BukkitQueue_0.class) {
                                    removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            // Set entities
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
            if (!entitiesToSpawn.isEmpty()) {
                synchronized (BukkitQueue_0.class) {
                    for (CompoundTag nativeTag : entitiesToSpawn) {
                        Map<String, Tag> entityTagMap = ReflectionUtils.getMap(nativeTag.getValue());
                        StringTag idTag = (StringTag) entityTagMap.get("Id");
                        ListTag posTag = (ListTag) entityTagMap.get("Pos");
                        ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                        if (idTag == null || posTag == null || rotTag == null) {
                            Fawe.debug("Unknown entity tag: " + nativeTag);
                            continue;
                        }
                        double x = posTag.getDouble(0);
                        double y = posTag.getDouble(1);
                        double z = posTag.getDouble(2);
                        float yaw = rotTag.getFloat(0);
                        float pitch = rotTag.getFloat(1);
                        String id = idTag.getValue();
                        Entity entity = EntityTypes.a(nmsWorld, new MinecraftKey(id));
                        if (entity != null) {
                            UUID uuid = entity.getUniqueID();
                            entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                            entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                            if (nativeTag != null) {
                                NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_13.fromNative(nativeTag);
                                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                    tag.remove(name);
                                }
                                entity.f(tag);
                            }
                            entity.setLocation(x, y, z, yaw, pitch);
                            synchronized (BukkitQueue_0.class) {
                                nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                            }
//                                createdEntities.add(entity.getUniqueID());
                        }
                    }
                }
            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                int count = this.getCount(j);
                if (count == 0) {
                    continue;
                }
                int countAir = this.getAir(j);
                final int[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                ChunkSection section = sections[j];
                if (copy != null) {
                    copy.storeSection(section, j);
                }
                if (section == null) {
                    if (count == countAir) {
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        section = sections[j] = getParent().newChunkSection(j << 4, flag, null);
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array); // TODO set data
                        continue;
                    }
                } else if (count >= 4096) {
                    if (countAir >= 4096) {
                        sections[j] = null;
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array);
                        continue;
                    }
                }
                int by = j << 4;
                DataPaletteBlock<IBlockData> nibble = section.getBlocks();
                int nonEmptyBlockCount = 0;
                IBlockData existing;
                for (int y = 0; y < 16; y++) {
                    short[][] i1 = FaweCache.CACHE_J[y];
                    for (int z = 0; z < 16; z++) {
                        short[] i2 = i1[z];
                        for (int x= 0; x < 16; x++) {
                            int combinedId = array[i2[x]];
                            switch (combinedId) {
                                case 0:
                                    continue;
                                case 1:
                                case 2:
                                case 3:
                                    existing = nibble.a(x, y, z);
                                    if (!existing.isAir()) {
                                        if (existing.e() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                        nonEmptyBlockCount--;
                                    }
                                    nibble.setBlock(x, y, z, BukkitQueue_1_13.air);
                                    continue;
                                default:
                                    existing = nibble.a(x, y, z);
                                    if (!existing.isAir()) {
                                        if (existing.e() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                    } else {
                                        nonEmptyBlockCount++;
                                    }
                                    nibble.setBlock(x, y, z, getParent().IBD_CACHE[(int) combinedId]);
                            }
                        }
                    }
                }
                getParent().setCount(0, getParent().getNonEmptyBlockCount(section) + nonEmptyBlockCount, section);
            }

            // Trim tiles
            HashMap<BlockPosition, TileEntity> toRemove = null;
            if (!tiles.isEmpty()) {
                Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = tiles.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<BlockPosition, TileEntity> tile = iterator.next();
                    BlockPosition pos = tile.getKey();
                    int lx = pos.getX() & 15;
                    int ly = pos.getY();
                    int lz = pos.getZ() & 15;
                    int j = FaweCache.CACHE_I[ly][lz][lx];
                    int[] array = this.getIdArray(j);
                    if (array == null) {
                        continue;
                    }
                    int k = FaweCache.CACHE_J[ly][lz][lx];
                    if (array[k] != 0) {
                        if (toRemove == null) {
                            toRemove = new HashMap<>();
                        }
                        if (copy != null) {
                            copy.storeTile(tile.getValue(), tile.getKey());
                        }
                        toRemove.put(tile.getKey(), tile.getValue());
                    }
                }
                if (toRemove != null) {
                    synchronized (BukkitQueue_0.class) {
                        for (Map.Entry<BlockPosition, TileEntity> entry : toRemove.entrySet()) {
                            BlockPosition bp = entry.getKey();
                            TileEntity tile = entry.getValue();
                            nmsWorld.s(bp);
                            tiles.remove(bp);
                            tile.z();
                            tile.invalidateBlockCache();
                        }
                    }
                }
            }

            // Set biomes
            if (this.biomes != null) {
                if (copy != null) {
                    copy.storeBiomes(nmsChunk.getBiomeIndex());
                }
                BiomeBase[] currentBiomes = nmsChunk.getBiomeIndex();
                for (int i = 0 ; i < this.biomes.length; i++) {
                    BiomeType biome = this.biomes[i];
                    if (biome != null) {
                        currentBiomes[i] = biome;
                    }
                }
            }
            // Set tiles
            Map<Short, CompoundTag> tilesToSpawn = this.getTiles();
            if (!tilesToSpawn.isEmpty()) {
                for (Map.Entry<Short, CompoundTag> entry : tilesToSpawn.entrySet()) {
                    CompoundTag nativeTag = entry.getValue();
                    short blockHash = entry.getKey();
                    int x = (blockHash >> 12 & 0xF) + bx;
                    int y = (blockHash & 0xFF);
                    int z = (blockHash >> 8 & 0xF) + bz;
                    BlockPosition pos = new BlockPosition(x, y, z); // Set pos
                    synchronized (BukkitQueue_0.class) {
                        TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                        if (tileEntity != null) {
                            NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_13.fromNative(nativeTag);
                            tag.set("x", new NBTTagInt(x));
                            tag.set("y", new NBTTagInt(y));
                            tag.set("z", new NBTTagInt(z));
                            tileEntity.load(tag);
                        }
                    }
                }
            }
            // Change task
            if (copy != null) {
                getParent().getChangeTask().run(copy, this);
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
