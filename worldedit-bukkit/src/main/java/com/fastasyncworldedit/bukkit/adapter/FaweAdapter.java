package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.TreeGenerator;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.List;

/**
 * A base class for version-specific implementations of the BukkitImplAdapter
 *
 * @param <TAG>          the version-specific NBT tag type
 * @param <SERVER_LEVEL> the version-specific ServerLevel type
 */
public abstract class FaweAdapter<TAG, SERVER_LEVEL> extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<TAG> {


    // Folia - Start
    protected MethodHandle currentWorldData = getCurrentWorldData();

    protected Class<?> regionizedWorldData = getRegionitedWorldData();

    protected Field captureTreeGeneration = getCaptureTreeGeneration();

    protected Field captureBlockStates = getCaptureBlockStates();

    protected Field capturedBlockStates = getCapturedBlockStates();

    private final boolean folia;

    protected FaweAdapter() {
        boolean isFolia = false;
        try {
            // Assume API is present
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            isFolia = true;
        } catch (Exception unused) {

        }
        this.folia = isFolia;
    }

    @Override
    public boolean generateTree(
            final TreeGenerator.TreeType treeType,
            final EditSession editSession,
            BlockVector3 blockVector3,
            final World world
    ) {
        TreeType bukkitType = BukkitWorld.toBukkitTreeType(treeType);
        if (bukkitType == TreeType.CHORUS_PLANT) {
            // bukkit skips the feature gen which does this offset normally, so we have to add it back
            blockVector3 = blockVector3.add(BlockVector3.UNIT_Y);
        }
        BlockVector3 target = blockVector3;
        SERVER_LEVEL serverLevel = getServerLevel(world);
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
                    BukkitAdapter.adapt(blockState.getBlockData())
            );
        }
        return true;
    }

    protected abstract void preCaptureStates(SERVER_LEVEL serverLevel);

    protected abstract List<BlockState> getCapturedBlockStatesCopy(SERVER_LEVEL serverLevel);

    protected abstract void postCaptureBlockStates(SERVER_LEVEL serverLevel);

    protected abstract SERVER_LEVEL getServerLevel(World world);

    // Folia Support
    protected abstract MethodHandle getCurrentWorldData();

    private Class<?> getRegionitedWorldData() {
        try {
            regionizedWorldData = Class.forName("io.papermc.paper.threadedregions.RegionizedWorldData");
        } catch (ClassNotFoundException e) {
        }
        return null;
    }

    private Field getCaptureTreeGeneration() {
        Field captureTreeGeneration = null;
        if (regionizedWorldData != null) {
            try {
                captureTreeGeneration = regionizedWorldData.getDeclaredField("captureTreeGeneration");
                captureTreeGeneration.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
        }
        return captureTreeGeneration;
    }

    private Field getCaptureBlockStates() {
        Field captureBlockStates = null;
        if (regionizedWorldData != null) {
            try {
                captureBlockStates = regionizedWorldData.getDeclaredField("captureBlockStates");
                captureBlockStates.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
        }
        return captureBlockStates;
    }

    private Field getCapturedBlockStates() {
        Field capturedBlockStates = null;
        if (regionizedWorldData != null) {
            try {
                capturedBlockStates = regionizedWorldData.getDeclaredField("capturedBlockStates");
                capturedBlockStates.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
        }
        return capturedBlockStates;
    }

    protected boolean isFolia() {
        return this.folia;
    }

}
