package com.fastasyncworldedit.core.anvil;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.implementation.packet.ChunkPacket;
import com.fastasyncworldedit.core.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MCAWorld extends AbstractWorld {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final Int2ObjectOpenHashMap<MCAWorld> worldMap = new Int2ObjectOpenHashMap<>();
    private final String name;
    private final Path folder;
    private final Path regionFolder;
    private final Int2ObjectOpenHashMap<MCAFile> mcaFileCache = new Int2ObjectOpenHashMap<>();

    private MCAWorld(String name, Path folder) {
        this.name = name;
        this.folder = folder;
        this.regionFolder = folder.resolve("region");
    }

    /**
     * New MCAWorld instance.
     *
     * @param name World name
     */
    public static synchronized MCAWorld of(String name) {
        return of(name, Fawe.platform().getWorldsFolder().resolve(name));
    }

    /**
     * New MCAWorld instance.
     *
     * @param name   World name
     * @param folder World file/folder
     */
    public static synchronized MCAWorld of(String name, Path folder) {
        if (Fawe.platform().isWorldLoaded(name)) {
            throw new IllegalStateException("World " + name + " is loaded. Anvil operations cannot be completed on a loaded world.");
        }
        // World could be the same name but in a different folder
        int combinedHash = Objects.hash(name, folder);
        return worldMap.computeIfAbsent(combinedHash, (i) -> new MCAWorld(name, folder));
    }

    public Collection<MCAFile> getMCAs() {
        getRegionFileFiles().forEach(file -> {
            String[] split = file.getFileName().toString().split("\\.");
            short regionX = Short.parseShort(split[1]);
            short regionZ = Short.parseShort(split[2]);
            int paired = MathMan.pair(regionX, regionZ);
            mcaFileCache.computeIfAbsent(
                    paired,
                    (i) -> new MCAFile(regionX, regionZ, regionFolder.resolve("r." + regionX + "." + regionZ + ".mca"))
            );
        });
        return mcaFileCache.values();
    }

    public List<Path> getRegionFileFiles() {
        try {
            return Files.list(regionFolder).filter(p -> p.toString().endsWith(".mca")).toList();
        } catch (IOException e) {
            LOGGER.error("Error listing region files", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean setTile(final int x, final int y, final int z, final CompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameUnsafe() {
        return name;
    }

    public Path getFolder() {
        return folder;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(
            final BlockVector3 position,
            final B block,
            final SideEffectSet sideEffects
    ) throws WorldEditException {
        return false;
    }

    @Override
    public Set<SideEffect> applySideEffects(
            final BlockVector3 position,
            final BlockState previousType,
            final SideEffectSet sideEffectSet
    ) throws WorldEditException {
        return null;
    }

    @Override
    public boolean clearContainerBlockContents(final BlockVector3 position) {
        return false;
    }

    @Override
    public void dropItem(final Vector3 position, final BaseItemStack item) {

    }

    @Override
    public void simulateBlockMine(final BlockVector3 position) {

    }

    @Override
    public boolean generateTree(
            final TreeGenerator.TreeType type,
            final EditSession editSession,
            final BlockVector3 position
    ) throws MaxChangedBlocksException {
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return null;
    }

    @Override
    public void refreshChunk(final int chunkX, final int chunkZ) {
        throw new UnsupportedOperationException("Not supported in anvil operations.");
    }

    @Override
    public IChunkGet get(final int chunkX, final int chunkZ) {
        short regionX = (short) (chunkX >> 5);
        short regionZ = (short) (chunkZ >> 5);
        int paired = MathMan.pair(regionX, regionZ);
        MCAFile mca = mcaFileCache.computeIfAbsent(
                paired,
                (i) -> new MCAFile(regionX, regionZ, regionFolder.resolve("r." + regionX + "." + regionZ + ".mca"))
        );
        try {
            return mca.getChunk(chunkX, chunkZ);
        } catch (IOException e) {
            LOGGER.error("Error loading chunk. Creating empty chunk.", e);
            return mca.newChunk(chunkX, chunkZ);
        }
    }

    @Override
    public void sendFakeChunk(@Nullable final Player player, final ChunkPacket packet) {
        throw new UnsupportedOperationException("Not supported in anvil operations.");
    }

    @Override
    public synchronized void flush() {
        for (MCAFile mca : mcaFileCache.values()) {
            try {
                mca.close();
            } catch (IOException e) {
                LOGGER.error("Could not flush MCAFile {}", mca.getFile().getFileName(), e);
            }
        }
    }

}
