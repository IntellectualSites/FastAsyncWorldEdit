package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.exception.FaweException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Deletes unvisited MCA files and Chunks<br>
 * - This a global filter and cannot be used a selection<br>
 */
public class DeleteUninhabitedFilter extends MCAFilterCounter {
    private final long inhabitedTicks;
    private final long fileDurationMillis;
    private final long cutoffChunkAgeEpoch;
    private boolean debug = false;

    public DeleteUninhabitedFilter(long fileDurationMillis, long inhabitedTicks, long chunkInactivityMillis) {
        this.fileDurationMillis = fileDurationMillis;
        this.inhabitedTicks = inhabitedTicks;
        this.cutoffChunkAgeEpoch = System.currentTimeMillis() - chunkInactivityMillis;
    }

    public void enableDebug() {
        this.debug = true;
    }

    public long getInhabitedTicks() {
        return inhabitedTicks;
    }

    public long getFileDurationMillis() {
        return fileDurationMillis;
    }

    public long getCutoffChunkAgeEpoch() {
        return cutoffChunkAgeEpoch;
    }

    @Override
    public boolean appliesFile(Path path, BasicFileAttributes attr) {
        String name = path.getFileName().toString();
        String[] split = name.split("\\.");
        final int mcaX = Integer.parseInt(split[1]);
        final int mcaZ = Integer.parseInt(split[2]);
        File file = path.toFile();
        long lastModified = attr.lastModifiedTime().toMillis();
        if (lastModified > cutoffChunkAgeEpoch) {
            return false;
        }
        try {
            if (shouldDelete(file, attr, mcaX, mcaZ)) {
                if (debug) {
                    Fawe.debug("Deleting " + file + " as it was modified at " + new Date(lastModified) + " and you provided a threshold of " + new Date(cutoffChunkAgeEpoch));
                }
                file.delete();
                get().add(512 * 512 * 256);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public MCAFile applyFile(MCAFile mca) {
        try {
            ForkJoinPool pool = new ForkJoinPool();
            mca.init();
            filter(mca, pool);
            pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            mca.close(pool);
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean shouldDelete(File file, BasicFileAttributes attr, int mcaX, int mcaZ) throws IOException {
        long creation = attr.creationTime().toMillis();
        long modified = attr.lastModifiedTime().toMillis();
        if ((modified - creation < fileDurationMillis && modified > creation) || file.length() < 12288) {
            return true;
        }
        return false;
    }

    public boolean shouldDeleteChunk(MCAFile mca, int cx, int cz) {
        return true;
    }

    public void filter(MCAFile mca, ForkJoinPool pool) throws IOException {
        mca.forEachSortedChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
            @Override
            public void run(Integer x, Integer z, Integer offset, Integer size) {
                int bx = mca.getX() << 5;
                int bz = mca.getZ() << 5;
                int cx = bx + x;
                int cz = bz + z;
                if (shouldDeleteChunk(mca, cx, cz)) {
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mca.streamChunk(offset, new RunnableVal<NBTStreamer>() {
                                    @Override
                                    public void run(NBTStreamer value) {
                                        addReaders(mca, x, z, value);
                                    }
                                });
                            } catch (FaweException ignore) {
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    pool.submit(task);
                }
            }
        });
    }

    public void addReaders(MCAFile mca, int x, int z, NBTStreamer streamer) {
        streamer.addReader(".Level.InhabitedTime", new BiConsumer<Integer, Long>() {
            @Override
            public void accept(Integer index, Long value) {
                if (value <= inhabitedTicks) {
                    MCAChunk chunk = new MCAChunk(null, x, z);
                    if (debug) {
                        int cx = (mca.getX() << 5) + (x & 31);
                        int cz = (mca.getZ() << 5) + (z & 31);
                        Fawe.debug("Deleting chunk " + cx + "," + cz + " as it was only inhabited for " + value + " and passed all other checks");
                    }
                    chunk.setDeleted(true);
                    synchronized (mca) {
                        mca.setChunk(chunk);
                    }
                    get().add(16 * 16 * 256);
                }
            }
        });
    }
}
