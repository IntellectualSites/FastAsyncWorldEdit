package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.collection.IterableThreadLocal;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.util.MainUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

public abstract class MCAWriter {
    private File folder;
    private final int length;
    private final int width;
    private final int area;
    private int OX, OZ;


    public MCAWriter(int width, int length, File regionFolder) {
        this.folder = regionFolder;
        this.width = width;
        this.length = length;
        this.area = width * length;
    }

    public final File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public final int getWidth() {
        return width;
    }

    public final int getLength() {
        return length;
    }

    /**
     * Set the MCA file offset (each mca file is 512 blocks)
     * - A negative value will shift the map negative
     * - This only applies to generation, not block get/set
     *
     * @param mcaOX
     * @param mcaOZ
     */
    public void setMCAOffset(int mcaOX, int mcaOZ) {
        OX = mcaOX << 9;
        OZ = mcaOZ << 9;
    }

    public int getOffsetX() {
        return OX;
    }

    public int getOffsetZ() {
        return OZ;
    }

    public final int getArea() {
        return area;
    }

    public abstract boolean shouldWrite(int chunkX, int chunkZ);

    public abstract MCAChunk write(MCAChunk input, int startX, int endX, int startZ, int endZ);

    public void generate() throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final ForkJoinPool pool = new ForkJoinPool();
        int bcx = 0;
        int bcz = 0;
        int tcx = (width - 1) >> 4;
        int tcz = (length - 1) >> 4;
        final ThreadLocal<MCAChunk> chunkStore = new ThreadLocal<MCAChunk>() {
            @Override
            protected MCAChunk initialValue() {
                MCAChunk chunk = new MCAChunk(null, 0, 0);
                chunk.biomes = new byte[256];
                return chunk;
            }
        };
        final ThreadLocal<byte[]> byteStore1 = new ThreadLocal<byte[]>() {
            @Override
            protected byte[] initialValue() {
                return new byte[500000];
            }
        };
        final ThreadLocal<byte[]> byteStore2 = new ThreadLocal<byte[]>() {
            @Override
            protected byte[] initialValue() {
                return new byte[500000];
            }
        };
        final ThreadLocal<Deflater> deflateStore = new ThreadLocal<Deflater>() {
            @Override
            protected Deflater initialValue() {
                Deflater deflater = new Deflater(Deflater.BEST_SPEED, false);
                return deflater;
            }
        };
        byte[] fileBuf = new byte[1 << 16];
        int mcaXMin = 0;
        int mcaZMin = 0;
        int mcaXMax = mcaXMin + ((width - 1) >> 9);
        int mcaZMax = mcaZMin + ((length - 1) >> 9);

        for (int mcaZ = mcaXMin; mcaZ <= mcaZMax; mcaZ++) {
            for (int mcaX = mcaXMin; mcaX <= mcaXMax; mcaX++) {
                final int fmcaX = mcaX;
                final int fmcaZ = mcaZ;
                File file = new File(folder, "r." + (mcaX + (getOffsetX() >> 9)) + "." + (mcaZ + (getOffsetZ() >> 9)) + ".mca");
                if (!file.exists()) {
                    file.createNewFile();
                }
                final BufferedRandomAccessFile raf = new BufferedRandomAccessFile(file, "rw", fileBuf);
                final byte[] header = new byte[4096];
                final byte[][] compressed = new byte[1024][];
                int bx = mcaX << 9;
                int bz = mcaZ << 9;
                int scx = bx >> 4;
                int ecx = Math.min(scx + 31, tcx);
                int scz = bz >> 4;
                int ecz = Math.min(scz + 31, tcz);
                for (int cz = scz; cz <= ecz; cz++) {
                    final int csz = cz << 4;
                    final int cez = Math.min(csz + 15, length - 1);
                    for (int cx = scx; cx <= ecx; cx++) {
                        final int csx = cx << 4;
                        final int cex = Math.min(csx + 15, width - 1);
                        final int fcx = cx;
                        final int fcz = cz;
                        if (shouldWrite(cx, cz)) {
                            pool.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        MCAChunk chunk = chunkStore.get();
                                        chunk.setLoc(null, fcx, fcz);
                                        chunk = write(chunk, csx, cex, csz, cez);
                                        if (chunk != null) {
                                            // Generation offset
                                            chunk.setLoc(null, fcx + (getOffsetX() >> 4), fcz + (getOffsetZ() >> 4));
                                            // Compress
                                            byte[] bytes = chunk.toBytes(byteStore1.get());
                                            byte[] compressedBytes = MainUtil.compress(bytes, byteStore2.get(), deflateStore.get());
                                            int blocks = (compressed.length + 4095) >> 12;
                                            compressed[((fcx & 31)) + ((fcz & 31) << 5)] = compressedBytes.clone();
                                        }
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
                pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int totalLength = 8192;
                            for (int i = 0; i < compressed.length; i++) {
                                byte[] compressedBytes = compressed[i];
                                if (compressedBytes != null) {
                                    int blocks = ((4095 + compressedBytes.length + 5) / 4096) * 4096;
                                    totalLength += blocks;
                                }
                            }
                            raf.setLength(totalLength);
                            int offset = 8192;
                            for (int i = 0; i < compressed.length; i++) {
                                byte[] compressedBytes = compressed[i];
                                if (compressedBytes != null) {
                                    // Set header
                                    int index = i << 2;
                                    int offsetMedium = offset >> 12;
                                    int blocks = ((4095 + compressedBytes.length + 5) / 4096);
                                    header[index] = (byte) (offsetMedium >> 16);
                                    header[index + 1] = (byte) ((offsetMedium >> 8));
                                    header[index + 2] = (byte) ((offsetMedium >> 0));
                                    header[index + 3] = (byte) (blocks);
                                    // Write bytes
                                    int cx = (fmcaX << 5) + (i & 31);
                                    int cz = (fmcaZ << 5) + (i >> 5);
                                    raf.seek(offset);
                                    raf.writeInt(compressedBytes.length + 1);
                                    raf.write(2);
                                    raf.write(compressedBytes);
                                    offset += blocks * 4096;
                                }
                            }
                            raf.seek(0);
                            raf.write(header);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                raf.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        pool.shutdown();
        IterableThreadLocal.clean(byteStore1);
        IterableThreadLocal.clean(byteStore2);
        IterableThreadLocal.clean(deflateStore);
    }
}
