package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;

/**
 * MCAQueue.filterWorld(MCAFilter)<br>
 * - Read and modify the world
 */
public class MCAFilter<T> extends IterableThreadLocal<T> {

    public void withPool(ForkJoinPool pool, MCAQueue queue) {
        return;
    }

    /**
     * Check whether this .mca file should be read
     * @param path
     * @param attr
     * @return
     */
    public boolean appliesFile(Path path, BasicFileAttributes attr) {
        return true;
    }

    /**
     * Check whether a .mca file should be read
     *
     * @param mcaX
     * @param mcaZ
     * @return
     */
    public boolean appliesFile(int mcaX, int mcaZ) {
        return true;
    }

    /**
     * Do something with the MCAFile<br>
     * - Return null if you don't want to filter chunks<br>
     * - Return the same file if you do want to filter chunks<br>
     *
     * @param file
     * @return file or null
     */
    public MCAFile applyFile(MCAFile file) {
        return file;
    }

    /**
     * Check whether a chunk should be read
     *
     * @param cx
     * @param cz
     * @return
     */
    public boolean appliesChunk(int cx, int cz) {
        return true;
    }

    /**
     * Do something with the MCAChunk<br>
     * - Return null if you don't want to filter blocks<br>
     * - Return the chunk if you do want to filter blocks<br>
     *
     * @param chunk
     * @return
     */
    public MCAChunk applyChunk(MCAChunk chunk, T cache) {
        return chunk;
    }

    /**
     * Make changes to the block here<br>
     * - e.g. block.setId(...)<br>
     * - Note: Performance is critical here<br>
     *
     * @param x
     * @param y
     * @param z
     * @param block
     */
    public void applyBlock(int x, int y, int z, BaseBlock block, T cache) {
    }

    /**
     * Do something with the MCAChunk after block filtering<br>
     *
     * @param chunk
     * @param cache
     * @return
     */
    public void finishChunk(MCAChunk chunk, T cache) {
    }

    /**
     * Do something with the MCAFile after block filtering<br>
     *
     * @param file
     * @param cache
     * @return
     */
    public void finishFile(MCAFile file, T cache) {
    }
}
