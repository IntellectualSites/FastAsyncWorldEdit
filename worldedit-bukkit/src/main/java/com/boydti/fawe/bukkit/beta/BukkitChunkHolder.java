package com.boydti.fawe.bukkit.beta;

import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IGetBlocks;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.beta.implementation.blocks.CharSetBlocks;
import com.boydti.fawe.beta.implementation.holder.ChunkHolder;
import com.google.common.util.concurrent.Futures;
import net.jpountz.util.UnsafeUtils;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkSection;
import org.bukkit.World;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class BukkitChunkHolder<T extends Future<T>> extends ChunkHolder {
    @Override
    public void init(final IQueueExtent extent, final int X, final int Z) {
        super.init(extent, X, Z);
    }

    @Override
    public IGetBlocks get() {
        BukkitQueue extent = (BukkitQueue) getExtent();
        return new BukkitGetBlocks(extent.getNmsWorld(), getX(), getZ());
    }

    @Override
    public T call() {
        BukkitQueue extent = (BukkitQueue) getExtent();
        BukkitGetBlocks get = (BukkitGetBlocks) getOrCreateGet();
        CharSetBlocks set = (CharSetBlocks) getOrCreateSet();
        int X = getX();
        int Z = getZ();

        Chunk nmsChunk = extent.ensureLoaded(X, Z);
        try {
            synchronized (nmsChunk) {
                ChunkSection[] sections = nmsChunk.getSections();
                World world = extent.getBukkitWorld();
                boolean hasSky = world.getEnvironment() == World.Environment.NORMAL;

                for (int layer = 0; layer < 16; layer++) {
                    if (!set.hasSection(layer)) continue;
                    char[] setArr = set.blocks[layer];
                    ChunkSection newSection;
                    ChunkSection existingSection = sections[layer];
                    if (existingSection == null) {
                        newSection = extent.newChunkSection(layer, hasSky, setArr);
                        if (BukkitQueue.setSectionAtomic(sections, null, newSection, layer)) {
                            continue;
                        } else {
                            existingSection = sections[layer];
                            if (existingSection == null) {
                                System.out.println("Skipping invalid null section. chunk:" + X + "," + Z + " layer: " + layer);
                                continue;
                            }
                        }
                    }
                    DelegateLock lock = BukkitQueue.applyLock(existingSection);
                    synchronized (lock) {
                        if (lock.isLocked()) {
                            lock.lock();
                            lock.unlock();
                        }
                        synchronized (get) {
                            ChunkSection getSection;
                            if (get.nmsChunk != nmsChunk) {
                                get.nmsChunk = nmsChunk;
                                get.sections = null;
                                get.reset();
                                System.out.println("chunk doesn't match");
                            } else {
                                getSection = get.getSections()[layer];
                                if (getSection != existingSection) {
                                    get.sections[layer] = existingSection;
                                    get.reset();
                                    System.out.println("Section doesn't match");
                                } else if (lock.isModified()) {
                                    System.out.println("lock is outdated");
                                    get.reset(layer);
                                }
                            }
                            char[] getArr = get.load(layer);
                            for (int i = 0; i < 4096; i++) {
                                char value = setArr[i];
                                if (value != 0) {
                                    getArr[i] = value;
                                }
                            }
                            newSection = extent.newChunkSection(layer, hasSky, getArr);
                            if (!BukkitQueue.setSectionAtomic(sections, existingSection, newSection, layer)) {
                                System.out.println("Failed to set chunk section:" + X + "," + Z + " layer: " + layer);
                                continue;
                            }
                        }
                    }
                }
            }
        } finally {
            extent.returnToPool(this);
        }
        /*

 - getBlocks
 - set ChunkSection lock with a tracking lock
 - synchronize on chunk object (so no other FAWE thread updates it at the same time)
 - verify cached section is same object as NMS chunk section
	otherwise, fetch the new section, set the tracking lock and reconstruct the getBlocks array
 - Merge raw getBlocks and setBlocks array
 - Construct the ChunkSection
 - In parallel on the main thread
 - if the tracking lock has had no updates and the cached ChunkSection == the NMS chunk section
 - Otherwise, reconstruct the ChunkSection (TODO: Benchmark if this is a performance concern)
 - swap in the new ChunkSection
 - Update tile entities/entities (if necessary)
 - Merge the biome array (if necessary)
 - set chunk status to needs relighting
 - mark as dirty

         */

//        throw new UnsupportedOperationException("Not implemented");
//        return true;
        return null;
//        return (T) (Future) Futures.immediateFuture(null);
    }

    @Override
    public void set(final Filter filter) {
        // for each block
        // filter.applyBlock(block)
        throw new UnsupportedOperationException("Not implemented");
    }
}