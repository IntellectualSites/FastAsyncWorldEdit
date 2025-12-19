package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.bukkit.util.FoliaLibHolder;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.TreeGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A base class for version-specific implementations of the BukkitImplAdapter
 *
 * @param <TAG>          the version-specific NBT tag type
 * @param <SERVER_LEVEL> the version-specific ServerLevel type
 */
public abstract class FaweAdapter<TAG, SERVER_LEVEL> extends CachedBukkitAdapter
        implements IDelegateBukkitImplAdapter<TAG> {

    protected final BukkitImplAdapter<TAG> parent;
    protected int[] ibdToOrdinal = null;
    protected int[] ordinalToIbdID = null;
    protected boolean initialised = false;
    protected Map<String, List<Property<?>>> allBlockProperties = null;

    protected FaweAdapter(final BukkitImplAdapter<TAG> parent) {
        this.parent = parent;
    }

    @Override
    public boolean generateTree(
            final TreeGenerator.TreeType treeType,
            final EditSession editSession,
            BlockVector3 blockVector3,
            final World world) {
        TreeType bukkitType = BukkitWorld.toBukkitTreeType(treeType);
        if (bukkitType == TreeType.CHORUS_PLANT) {
            // bukkit skips the feature gen which does this offset normally, so we have to
            // add it back
            blockVector3 = blockVector3.add(BlockVector3.UNIT_Y);
        }
        BlockVector3 target = blockVector3;
        SERVER_LEVEL serverLevel = getServerLevel(world);

        if (FoliaLibHolder.isFolia()) {
            return generateTreeFolia(bukkitType, editSession, target, world);
        }

        List<BlockState> placed = TaskManager.taskManager().sync(() -> {
            preCaptureStates(serverLevel);
            try {
                if (!world.generateTree(BukkitAdapter.adapt(world, target), bukkitType)) {
                    return null;
                }
                return getCapturedBlockStatesCopy(serverLevel);
            } finally {
                postCaptureBlockStates(serverLevel);
            }
        });

        if (placed == null || placed.isEmpty()) {
            return false;
        }
        for (BlockState blockState : placed) {
            if (blockState == null || blockState.getType() == Material.AIR) {
                continue;
            }
            editSession.setBlock(blockState.getX(), blockState.getY(), blockState.getZ(),
                    BukkitAdapter.adapt(blockState.getBlockData()));
        }
        return true;
    }

    private boolean generateTreeFolia(
            TreeType treeType,
            EditSession editSession,
            BlockVector3 target,
            World world) {
        if (Bukkit.isOwnedByCurrentRegion(world, target.x() >> 4, target.z() >> 4)) {
            return generateTreeFoliaInternal(treeType, editSession, target, world);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        org.bukkit.Location location = new org.bukkit.Location(world, target.x(), target.y(), target.z());
        FoliaLibHolder.getScheduler().runAtLocation(
                location,
                task -> {
                    try {
                        boolean result = generateTreeFoliaInternal(treeType, editSession, target, world);
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate tree on Folia", e);
        }
    }

    private boolean generateTreeFoliaInternal(
            TreeType treeType,
            EditSession editSession,
            BlockVector3 target,
            World world) {
        Set<BlockVector3> beforeBlocks = new HashSet<>();

        int radius = 10;
        int height = 32;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockVector3 pos = target.add(x, y, z);
                    org.bukkit.block.Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                    if (block.getType() != Material.AIR) {
                        beforeBlocks.add(pos);
                    }
                }
            }
        }

        boolean generated = world.generateTree(BukkitAdapter.adapt(world, target), treeType);

        if (!generated) {
            return false;
        }

        List<BlockState> newBlocks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= height; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockVector3 pos = target.add(x, y, z);
                    if (!beforeBlocks.contains(pos)) {
                        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                        if (block.getType() != Material.AIR) {
                            newBlocks.add(block.getState());
                        }
                    }
                }
            }
        }

        for (BlockState blockState : newBlocks) {
            editSession.setBlock(blockState.getX(), blockState.getY(), blockState.getZ(),
                    BukkitAdapter.adapt(blockState.getBlockData()));
        }

        return !newBlocks.isEmpty();
    }

    public void mapFromGlobalPalette(char[] data) {
        assert data.length == 4096;
        ensureInit();
        for (int i = 0; i < 4096; i++) {
            data[i] = (char) this.ibdToOrdinal[data[i]];
        }
    }

    public void mapWithPalette(char[] data, char[] paletteToOrdinal) {
        for (int i = 0; i < 4096; i++) {
            char paletteVal = data[i];
            char val = paletteToOrdinal[paletteVal];
            assert val != Character.MAX_VALUE; // paletteToOrdinal should prevent that
            data[i] = val;
        }
    }

    protected abstract void ensureInit();

    protected abstract void preCaptureStates(SERVER_LEVEL serverLevel);

    protected abstract List<BlockState> getCapturedBlockStatesCopy(SERVER_LEVEL serverLevel);

    protected abstract void postCaptureBlockStates(SERVER_LEVEL serverLevel);

    protected abstract SERVER_LEVEL getServerLevel(World world);

}
