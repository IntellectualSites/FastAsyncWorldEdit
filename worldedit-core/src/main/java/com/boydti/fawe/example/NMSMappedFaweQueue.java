package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class NMSMappedFaweQueue<WORLD, CHUNK, CHUNKSECTION, SECTION> extends MappedFaweQueue<WORLD, CHUNK, CHUNKSECTION, SECTION> {

    private final int maxY;

    public NMSMappedFaweQueue(World world) {
        super(world);
        this.maxY = world.getMaxY();
    }

    public NMSMappedFaweQueue(String world) {
        super(world);
        this.maxY = 256;
    }

    public NMSMappedFaweQueue(String world, IFaweQueueMap map) {
        super(world, map);
        this.maxY = 256;
    }

    public NMSMappedFaweQueue(World world, IFaweQueueMap map) {
        super(world, map);
        this.maxY = world.getMaxY();
    }

    @Override
    public void runTasks() {
        super.runTasks();
        if (!getRelighter().isEmpty()) {
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    if (getSettings().IMP.LIGHTING.REMOVE_FIRST) {
                        getRelighter().removeAndRelight(hasSky());
                    } else {
                        getRelighter().fixLightingSafe(hasSky());
                    }
                }
            });
        }
    }

    private final Relighter relighter = getSettings().IMP.LIGHTING.MODE > 0 ? new NMSRelighter(this) : NullRelighter.INSTANCE;

    @Override
    public Relighter getRelighter() {
        return relighter;
    }

    @Override
    public void end(FaweChunk chunk) {
        super.end(chunk);
        if (getSettings().IMP.LIGHTING.MODE == 0) {
            sendChunk(chunk);
            return;
        }
        if (!getSettings().IMP.LIGHTING.DELAY_PACKET_SENDING) {
            sendChunk(chunk);
        }
        if (getSettings().IMP.LIGHTING.MODE == 2) {
            getRelighter().addChunk(chunk.getX(), chunk.getZ(), null, chunk.getBitMask());
            return;
        }
        IntFaweChunk cfc = (IntFaweChunk) chunk;
        boolean relight = false;
        byte[] fix = new byte[(maxY + 1) >> 4];
        boolean sky = hasSky();
        if (sky) {
            for (int i = cfc.ids.length - 1; i >= 0; i--) {
                int air = cfc.getAir(i);
                int solid = cfc.getCount(i);
                if (air == 4096) {
                    fix[i] = Relighter.SkipReason.AIR;
                } else if (air == 0 && solid == 4096) {
                    fix[i] = Relighter.SkipReason.SOLID;
                } else if (solid == 0 && relight == false) {
                    fix[i] = Relighter.SkipReason.AIR;
                } else {
                    fix[i] = Relighter.SkipReason.NONE;
                    relight = true;
                }
            }
        }
        if (relight) {
            getRelighter().addChunk(chunk.getX(), chunk.getZ(), fix, chunk.getBitMask());
        } else if (getSettings().IMP.LIGHTING.DELAY_PACKET_SENDING) {
            sendChunk(chunk);
        }
    }

    @Override
    public void sendChunk(final FaweChunk fc) {
        try {
            refreshChunk(fc);
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
    }

    public abstract void setHeightMap(FaweChunk chunk, byte[] heightMap);

    public abstract void setFullbright(CHUNKSECTION sections);

    public boolean removeLighting(CHUNKSECTION sections, RelightMode mode, boolean hasSky) {
        boolean result = false;
        for (int i = 0; i < 16; i++) {
            SECTION section = getCachedSection(sections, i);
            if (section != null) {
                result |= removeSectionLighting(section, i, hasSky);
            }
        }
        return result;
    }

    public abstract boolean removeSectionLighting(SECTION sections, int layer, boolean hasSky);

    public boolean isSurrounded(final char[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int id) {
        return !BlockTypes.get(id).getMaterial().isTranslucent();
    }

    public int getId(final char[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return BlockTypes.AIR.getInternalId();
        }
        if ((y < 0) || (y > maxY)) {
            return BlockTypes.AIR.getInternalId();
        }
        final int i = FaweCache.CACHE_I[y][z][x];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][z][x];
        return section[j] >> 4;
    }

    public void saveChunk(CHUNK chunk) {
    }

    public abstract void relight(int x, int y, int z);

    public abstract void relightBlock(int x, int y, int z);

    public abstract void relightSky(int x, int y, int z);

    public void setSkyLight(int x, int y, int z, int value) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return;
            }
        }
        if (lastSection == null) {
            return;
        }
        setSkyLight(lastSection, x, y, z, value);
    }

    public void setBlockLight(int x, int y, int z, int value) {
        int cx = x >> 4;
        int cz = z >> 4;
        int cy = y >> 4;
        if (cx != lastSectionX || cz != lastSectionZ) {
            lastSectionX = cx;
            lastSectionZ = cz;
            lastChunk = ensureChunkLoaded(cx, cz);
            if (lastChunk != null) {
                lastChunkSections = getSections(lastChunk);
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                lastChunkSections = null;
                return;
            }
        } else if (cy != lastSectionY) {
            if (lastChunkSections != null) {
                lastSection = getCachedSection(lastChunkSections, cy);
            } else {
                return;
            }
        }
        if (lastSection == null) {
            return;
        }
        setBlockLight(lastSection, x, y, z, value);
    }

    public abstract void setSkyLight(SECTION section, int x, int y, int z, int value);

    public abstract void setBlockLight(SECTION section, int x, int y, int z, int value);

    public abstract void refreshChunk(FaweChunk fs);

    public abstract IntFaweChunk getPrevious(IntFaweChunk fs, CHUNKSECTION sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception;
}
