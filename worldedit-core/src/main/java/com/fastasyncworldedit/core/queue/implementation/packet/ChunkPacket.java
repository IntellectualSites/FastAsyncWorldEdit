package com.fastasyncworldedit.core.queue.implementation.packet;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.internal.io.FastByteArrayOutputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import com.fastasyncworldedit.core.queue.IBlocks;
import com.sk89q.jnbt.CompoundTag;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ChunkPacket implements Function<byte[], byte[]>, Supplier<byte[]> {

    private final boolean full;
    private final Supplier<IBlocks> chunkSupplier;
    private IBlocks chunk;

    private int chunkX;
    private int chunkZ;
    private byte[] sectionBytes;
    private Object nativePacket;

    public ChunkPacket(int chunkX, int chunkZ, Supplier<IBlocks> chunkSupplier, boolean replaceAllSections) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkSupplier = chunkSupplier;
        this.full = replaceAllSections;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public synchronized void setPosition(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        nativePacket = null;
    }

    public boolean isFull() {
        return full;
    }

    public IBlocks getChunk() {
        if (this.chunk == null) {
            synchronized (this) {
                if (this.chunk == null) {
                    this.chunk = chunkSupplier.get();
                }
            }
        }
        return chunk;
    }

    public byte[] getSectionBytes() {
        byte[] tmp = this.sectionBytes;
        if (tmp == null) {
            synchronized (this) {
                if (sectionBytes == null) {
                    IBlocks tmpChunk = getChunk();
                    byte[] buf = FaweCache.INSTANCE.BYTE_BUFFER_8192.get();
                    sectionBytes = tmpChunk.toByteArray(buf, tmpChunk.getBitMask(), this.full, true);
                }
                tmp = sectionBytes;
            }
        }
        return tmp;
    }


    public Object getNativePacket() {
        return nativePacket;
    }

    public void setNativePacket(Object nativePacket) {
        this.nativePacket = nativePacket;
    }

    @Override
    @Deprecated
    public byte[] get() {
        return apply(FaweCache.INSTANCE.BYTE_BUFFER_8192.get());
    }

    public CompoundTag getHeightMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("MOTION_BLOCKING", new long[36]);
        // TODO
        return FaweCache.INSTANCE.asTag(map);
    }

    @Override
    public byte[] apply(byte[] buffer) {
        try {
            byte[] sectionBytes = getSectionBytes();

            FastByteArrayOutputStream baos = new FastByteArrayOutputStream(buffer);
            FaweOutputStream fos = new FaweOutputStream(baos);

            fos.writeInt(this.chunkX);
            fos.writeInt(this.chunkZ);

            fos.writeBoolean(this.full);

            fos.writeVarInt(getChunk().getBitMask());

            fos.writeNBT("", getHeightMap());

            fos.writeVarInt(sectionBytes.length);
            fos.write(sectionBytes);
            // TODO entities / NBT
            //Set<CompoundTag> entities = chunk.getEntities();
            //Map<BlockVector3, CompoundTag> tiles = chunk.getTiles();
            fos.writeVarInt(0); // (Entities / NBT)
            return baos.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
