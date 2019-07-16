package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.adapter.v1_13_1.BlockMaterial_1_13;
import com.boydti.fawe.bukkit.adapter.v1_13_1.Spigot_v1_13_R2;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.server.v1_13_R2.BiomeBase;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Blocks;
import net.minecraft.server.v1_13_R2.ChunkSection;
import net.minecraft.server.v1_13_R2.DataBits;
import net.minecraft.server.v1_13_R2.DataPalette;
import net.minecraft.server.v1_13_R2.DataPaletteBlock;
import net.minecraft.server.v1_13_R2.DataPaletteHash;
import net.minecraft.server.v1_13_R2.DataPaletteLinear;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.NibbleArray;
import net.minecraft.server.v1_13_R2.RegistryID;
import net.minecraft.server.v1_13_R2.TileEntity;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.boydti.fawe.bukkit.v0.BukkitQueue_0.getAdapter;
import static com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13.fieldRegistryb;
import static com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13.fieldRegistryc;
import static com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13.fieldRegistryd;
import static com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13.fieldRegistrye;
import static com.boydti.fawe.bukkit.v1_13.BukkitQueue_1_13.fieldRegistryf;

public class BukkitChunk_1_13 extends IntFaweChunk<Chunk, BukkitQueue_1_13> {

    public ChunkSection[] sectionPalettes;

    private static final IBlockData AIR = ((BlockMaterial_1_13) BlockTypes.AIR.getMaterial()).getState();

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

    public BukkitChunk_1_13(FaweQueue parent, int x, int z, int[][] ids, short[] count, short[] air) {
        super(parent, x, z, ids, count, air);
    }

    public void storeBiomes(BiomeBase[] biomes) {
        if (biomes != null) {
            if (this.biomes == null) {
                this.biomes = new BiomeType[256];
            }
            for (int i = 0; i < 256; i++) {
                this.biomes[i] = BukkitAdapter.adapt(CraftBlock.biomeBaseToBiome(biomes[i]));
            }
        }
    }

    @Override
    public int[][] getCombinedIdArrays() {
        if (this.sectionPalettes != null) {
            for (int i = 0; i < setBlocks.length; i++) {
                getIdArray(i);
            }
        }
        return this.setBlocks;
    }

    @Override
    public int[] getIdArray(int layer) {
        if (this.setBlocks[layer] == null && this.sectionPalettes != null) {
            ChunkSection section = this.sectionPalettes[layer];
            int[] idsArray = this.setBlocks[layer];
            if (section != null && idsArray == null) {
                this.setBlocks[layer] = idsArray = new int[4096];
                if (!section.a()) {
                    try {
                        DataPaletteBlock<IBlockData> blocks = section.getBlocks();
                        DataBits bits = (DataBits) BukkitQueue_1_13.fieldBits.get(blocks);
                        DataPalette<IBlockData> palette = (DataPalette<IBlockData>) BukkitQueue_1_13.fieldPalette.get(blocks);

                        long[] raw = bits.a();
                        int bitsPerEntry = bits.c();

                        new BitArray4096(raw, bitsPerEntry).toRaw(idsArray);
                        IBlockData defaultBlock = (IBlockData) BukkitQueue_1_13.fieldDefaultBlock.get(blocks);
                        // TODO optimize away palette.a
                        for (int i = 0; i < 4096; i++) {
                            IBlockData ibd = palette.a(idsArray[i]);
                            if (ibd == null) {
                                ibd = defaultBlock;
                            }
                            int ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToInt(ibd);
                            idsArray[i] = BlockTypes.states[ordinal].getInternalId();
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return this.setBlocks[layer];
    }

    public boolean storeTile(TileEntity tile, BlockPosition pos) {
        CompoundTag nativeTag = getParent().getTag(tile);
        setTile(pos.getX() & 15, pos.getY(), pos.getZ() & 15, nativeTag);
        return true;
    }

    public boolean storeEntity(Entity ent) throws InvocationTargetException, IllegalAccessException {
        if (ent instanceof EntityPlayer || BukkitQueue_0.getAdapter() == null) {
            return false;
        }
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

    public boolean storeSection(ChunkSection section, int layer) throws IllegalAccessException {
        if (sectionPalettes == null) {
            // TODO optimize don't copy light
            sectionPalettes = new ChunkSection[16];
        }
        sectionPalettes[layer] = section;
        return true;
    }

    public ChunkSection copy(ChunkSection current) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        ChunkSection newSection = new ChunkSection(current.getYPosition(), current.getSkyLightArray() != null);

        // Copy light
        NibbleArray skyLight = current.getSkyLightArray();
        NibbleArray blockLight = current.getEmittedLightArray();

        NibbleArray newBlockLight = newSection.getEmittedLightArray();
        NibbleArray newSkyLight = newSection.getSkyLightArray();

        byte[] newBlockBytes = newBlockLight.asBytes();
        byte[] blockLightBytes = blockLight.asBytes();
        for (int i = 0; i < 2048; i++) newBlockBytes[i] = blockLightBytes[i];
        if (skyLight != null) {
            byte[] newSkyBytes = newSkyLight.asBytes();
            byte[] skyLightBytes = skyLight.asBytes();
            for (int i = 0; i < 2048; i++) newSkyBytes[i] = skyLightBytes[i];
        }

        // Copy counters
        Object nonEmptyBlockCount = BukkitQueue_1_13.fieldNonEmptyBlockCount.get(current);
        BukkitQueue_1_13.fieldNonEmptyBlockCount.set(newSection, nonEmptyBlockCount);

        Object tickingBlockCount = BukkitQueue_1_13.fieldTickingBlockCount.get(current);
        BukkitQueue_1_13.fieldTickingBlockCount.set(newSection, tickingBlockCount);

        Object liquidCount = BukkitQueue_1_13.fieldLiquidCount.get(current);
        BukkitQueue_1_13.fieldLiquidCount.set(newSection, liquidCount);

        // Copy blocks
        DataPaletteBlock<IBlockData> blocks = current.getBlocks();
        DataPaletteBlock<IBlockData> blocksCopy = copy(blocks);
        BukkitQueue_1_13.fieldSection.set(newSection, blocksCopy);

        return newSection;
    }

    public DataPaletteBlock<IBlockData> copy(DataPaletteBlock current) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        // Clone palette
        DataPalette currentPalette = (DataPalette) BukkitQueue_1_13.fieldPalette.get(current);
        DataPaletteBlock<IBlockData> paletteBlock = newDataPaletteBlock();
        int size = BukkitQueue_1_13.fieldSize.getInt(current);

        DataPalette<IBlockData> newPalette = currentPalette;
        if (currentPalette instanceof DataPaletteHash) {
            // TODO optimize resize
            newPalette = new DataPaletteHash<>(Block.REGISTRY_ID, size, paletteBlock, GameProfileSerializer::d, GameProfileSerializer::a);
            RegistryID<IBlockData> currReg = (RegistryID<IBlockData>) BukkitQueue_1_13.fieldHashBlocks.get(currentPalette);
            RegistryID<IBlockData> newReg = (RegistryID<IBlockData>) BukkitQueue_1_13.fieldHashBlocks.get(newPalette);
            int arrLen = 1 << size;
            System.arraycopy(fieldRegistryb.get(currReg), 0, fieldRegistryb.get(newReg), 0, arrLen);
            System.arraycopy(fieldRegistryc.get(currReg), 0, fieldRegistryc.get(newReg), 0, arrLen);
            System.arraycopy(fieldRegistryd.get(currReg), 0, fieldRegistryd.get(newReg), 0, arrLen);
            fieldRegistrye.set(newReg, fieldRegistrye.get(currReg));
            fieldRegistryf.set(newReg, fieldRegistryf.get(currReg));
        } else if (currentPalette instanceof DataPaletteLinear) {
            // TODO optimize resize
            newPalette = new DataPaletteLinear<>(Block.REGISTRY_ID, size, paletteBlock, GameProfileSerializer::d);
            Object[] currArray = ((Object[]) BukkitQueue_1_13.fieldLinearBlocks.get(currentPalette));
            Object[] newArray = ((Object[]) BukkitQueue_1_13.fieldLinearBlocks.get(newPalette));
            BukkitQueue_1_13.fieldLinearIndex.set(newPalette, BukkitQueue_1_13.fieldLinearIndex.get(currentPalette));
            for (int i = 0; i < newArray.length; i++) newArray[i] = currArray[i];
        }

        BukkitQueue_1_13.fieldPalette.set(paletteBlock, newPalette);
        // Clone size
        BukkitQueue_1_13.fieldSize.set(paletteBlock, size);
        // Clone palette
        DataBits currentBits = (DataBits) BukkitQueue_1_13.fieldBits.get(current);
        DataBits newBits = new DataBits(currentBits.c(), currentBits.b(), currentBits.a().clone());
        BukkitQueue_1_13.fieldBits.set(paletteBlock, newBits);

        // TODO copy only if different
        Object defaultBlock = BukkitQueue_1_13.fieldDefaultBlock.get(current);
        if (defaultBlock != AIR) {
            ReflectionUtils.setFailsafeFieldValue(BukkitQueue_1_13.fieldDefaultBlock, paletteBlock, BukkitQueue_1_13.fieldDefaultBlock.get(current));
        }

        return paletteBlock;
    }

    @Override
    public IntFaweChunk<Chunk, BukkitQueue_1_13> copy(boolean shallow) {
        BukkitChunk_1_13 copy;
        if (shallow) {
            copy = new BukkitChunk_1_13(getParent(), getX(), getZ(), setBlocks, count, air);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_13(getParent(), getX(), getZ(), (int[][]) MainUtil.copyNd(setBlocks), count.clone(), air.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
            copy.chunk = chunk;
        }
        if (sectionPalettes != null) {
            copy.sectionPalettes = new ChunkSection[16];
            try {
                for (int i = 0; i < sectionPalettes.length; i++) {
                    ChunkSection current = sectionPalettes[i];
                    if (current == null) {
                        continue;
                    }
                    copy.sectionPalettes[i] = copy(current);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return copy;
    }

    private DataPaletteBlock<IBlockData> newDataPaletteBlock() {
        return new DataPaletteBlock<>(ChunkSection.GLOBAL_PALETTE, Block.REGISTRY_ID, GameProfileSerializer::d, GameProfileSerializer::a, Blocks.AIR.getBlockData());
    }

    @Override
    public Chunk getNewChunk() {
        return getParent().getWorld().getChunkAt(getX(), getZ());
    }

    public void optimize() {
        if (sectionPalettes != null) {
            return;
        }
        int[][] arrays = getCombinedIdArrays();
        for (int layer = 0; layer < 16; layer++) {
            if (getCount(layer) > 0) {
                if (sectionPalettes == null) {
                    sectionPalettes = new ChunkSection[16];
                }
                int[] array = arrays[layer];
                sectionPalettes[layer] = BukkitQueue_1_13.newChunkSection(layer, getParent().hasSky(), array);
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
        Spigot_v1_13_R2 adapter = (Spigot_v1_13_R2) BukkitQueue_0.getAdapter();
        try {
            BukkitChunk_1_13 copy = getParent().getChangeTask() != null ? new BukkitChunk_1_13(getParent(), getX(), getZ()) : null;
            final Chunk chunk = this.getChunk();
            final World world = chunk.getWorld();
            Settings settings = getParent().getSettings();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;
            final boolean flag = world.getEnvironment() == World.Environment.NORMAL;
            net.minecraft.server.v1_13_R2.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            nmsChunk.f(true); // Set Modified
            nmsChunk.mustSave = true;
            nmsChunk.markDirty();
            net.minecraft.server.v1_13_R2.World nmsWorld = nmsChunk.world;
            ChunkSection[] sections = nmsChunk.getSections();
            List<Entity>[] entities = nmsChunk.getEntitySlices();
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            // Remove entities
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                for (Collection<Entity> ents : entities) {
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

                            int index = (((y & 0xF) << 8) | (z << 4) | x);
                            if (array[index] != 0) {
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
                            NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_13.fromNative(nativeTag);
                            for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                tag.remove(name);
                            }
                            entity.f(tag);
                            entity.setLocation(x, y, z, yaw, pitch);
                            synchronized (BukkitQueue_0.class) {
                                nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                            }
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
                    if (section != null) {
                        copy.storeSection(copy(section), j);
                    }
                }
                if (section == null) {
                    if (count == countAir) {
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        section = sections[j] = this.sectionPalettes[j];
                        continue;
                    } else {
                        section = sections[j] = getParent().newChunkSection(j, flag, array);
                        continue;
                    }
                } else if (count >= 4096 && false) {
                    if (countAir >= 4096) {
                        sections[j] = null;
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        section = sections[j] = this.sectionPalettes[j];
                        continue;
                    } else {
                        section = sections[j] = getParent().newChunkSection(j, flag, array);
                        continue;
                    }
                }
                int by = j << 4;
                DataPaletteBlock<IBlockData> nibble = section.getBlocks();
                int nonEmptyBlockCount = 0;
                IBlockData existing;

                for (int y = 0, i = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x= 0; x < 16; x++, i++) {
                            int combinedId = array[i];
                            switch (combinedId) {
                                case 0:
                                    continue;
                                case BlockID.AIR:
                                case BlockID.CAVE_AIR:
                                case BlockID.VOID_AIR:
                                    existing = nibble.a(x, y, z);
                                    if (!existing.isAir()) {
                                        if (existing.e() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                        nonEmptyBlockCount--;
                                        nibble.setBlock(x, y, z, AIR);
                                    }
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
                                    BlockState state = BlockState.getFromInternalId(combinedId);
                                    IBlockData ibd = ((BlockMaterial_1_13) state.getMaterial()).getState();
                                    nibble.setBlock(x, y, z, ibd);
                            }
                        }
                    }
                }
                getParent().setCount(0, getParent().getNonEmptyBlockCount(section) + nonEmptyBlockCount, section);
            }

            // Trim tiles
            HashMap<BlockPosition, TileEntity> toRemove = null;
            if (!tiles.isEmpty()) {
                for (Map.Entry<BlockPosition, TileEntity> tile : tiles.entrySet()) {
                    BlockPosition pos = tile.getKey();
                    int lx = pos.getX() & 15;
                    int ly = pos.getY();
                    int lz = pos.getZ() & 15;
                    int layer = ly >> 4;
                    int[] array = this.getIdArray(layer);
                    if (array == null) {
                        continue;
                    }
                    int index = (((ly & 0xF) << 8) | (lz << 4) | lx);
                    if (array[index] != 0) {
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
                            tile.y();
//                            nmsWorld.c.remove(bp);
//                            nmsWorld.tileEntityList.remove(bp);
//                            nmsWorld.tileEntityListTick.remove(bp);
                            tiles.remove(bp);
                            tile.z();
                            tile.invalidateBlockCache();
                        }
                    }
                }
            }

            // Set biomes
            if (this.biomes != null) {
                BiomeBase[] currentBiomes = nmsChunk.getBiomeIndex();
                if (copy != null) {
                    copy.storeBiomes(currentBiomes);
                }
                for (int i = 0 ; i < this.biomes.length; i++) {
                    BiomeType biome = this.biomes[i];
                    if (biome != null) {
                        Biome craftBiome = adapter.adapt(biome);
                        currentBiomes[i] = CraftBlock.biomeToBiomeBase(craftBiome);
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
            e.printStackTrace();
        }
        return this;
    }
}