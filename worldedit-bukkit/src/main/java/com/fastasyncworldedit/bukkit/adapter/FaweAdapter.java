package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.util.FoliaSupport;
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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.fastasyncworldedit.core.util.FoliaSupport.getRethrowing;
import static com.fastasyncworldedit.core.util.FoliaSupport.runRethrowing;
import static java.lang.invoke.MethodType.methodType;

/**
 * A base class for version-specific implementations of the BukkitImplAdapter
 *
 * @param <TAG>          the version-specific NBT tag type
 * @param <SERVER_LEVEL> the version-specific ServerLevel type
 */
public abstract class FaweAdapter<TAG, SERVER_LEVEL> extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<TAG> {

    private static final VarHandle CAPTURE_TREE_GENERATION;
    private static final VarHandle CAPTURE_BLOCK_STATES;
    private static final MethodHandle CAPTURED_BLOCK_STATES;
    private static final MethodHandle GET_CURRENT_WORLD_DATA;

    static {
        VarHandle captureTreeGeneration = null;
        VarHandle captureBlockStates = null;
        MethodHandle capturedBlockStates = null;
        MethodHandle getCurrentWorldData = null;
        if (FoliaSupport.isFolia()) {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                Class<?> regionizedWorldDataClass = Class.forName("io.papermc.paper.threadedregions.RegionizedWorldData");
                Class<?> serverLevelClass = regionizedWorldDataClass.getDeclaredField("world").getType();
                captureTreeGeneration = lookup.findVarHandle(regionizedWorldDataClass, "captureTreeGeneration", boolean.class);
                captureBlockStates = lookup.findVarHandle(regionizedWorldDataClass, "captureBlockStates", boolean.class);
                capturedBlockStates = lookup.findGetter(regionizedWorldDataClass, "capturedBlockStates", Map.class);
                getCurrentWorldData = lookup.findVirtual(
                        serverLevelClass,
                        "getCurrentWorldData",
                        methodType(regionizedWorldDataClass)
                );
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
                throw new AssertionError("Incompatible Folia version", e);
            }
        }
        CAPTURE_TREE_GENERATION = captureTreeGeneration;
        CAPTURE_BLOCK_STATES = captureBlockStates;
        CAPTURED_BLOCK_STATES = capturedBlockStates;
        GET_CURRENT_WORLD_DATA = getCurrentWorldData;
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
        List<BlockState> placed = TaskManager.taskManager().syncAt(() -> {
            preCaptureStatesCommon(serverLevel);
            try {
                if (!world.generateTree(BukkitAdapter.adapt(world, target), bukkitType)) {
                    return null;
                }
                return getCapturedBlockStatesCopyCommon(serverLevel);
            } finally {
                postCaptureBlockStatesCommon(serverLevel);
            }
        }, BukkitAdapter.adapt(world), blockVector3.getBlockX() >> 4, blockVector3.getBlockZ() >> 4);

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

    private void preCaptureStatesCommon(SERVER_LEVEL serverLevel) {
        if (FoliaSupport.isFolia()) {
            preCaptureStatesFolia(serverLevel);
        } else {
            preCaptureStates(serverLevel);
        }
    }

    private List<BlockState> getCapturedBlockStatesCopyCommon(SERVER_LEVEL serverLevel) {
        if (FoliaSupport.isFolia()) {
            return getCapturedBlockStatesCopyFolia(serverLevel);
        } else {
            return getCapturedBlockStatesCopy(serverLevel);
        }
    }

    private void postCaptureBlockStatesCommon(SERVER_LEVEL serverLevel) {
        if (FoliaSupport.isFolia()) {
            postCaptureBlockStatesFolia(serverLevel);
        } else {
            postCaptureBlockStates(serverLevel);
        }
    }

    private void preCaptureStatesFolia(SERVER_LEVEL serverLevel) {
        runRethrowing(() -> {
            Object currentWorldData = GET_CURRENT_WORLD_DATA.invoke(serverLevel);
            CAPTURE_TREE_GENERATION.set(currentWorldData, true);
            CAPTURE_BLOCK_STATES.set(currentWorldData, true);
        });
    }

    private List<BlockState> getCapturedBlockStatesCopyFolia(SERVER_LEVEL serverLevel) {
        return getRethrowing(() -> {
            Object currentWorldData = GET_CURRENT_WORLD_DATA.invoke(serverLevel);
            @SuppressWarnings("unchecked")
            var capturedBlockStates = (Map<?, BlockState>) CAPTURED_BLOCK_STATES.invoke(currentWorldData);
            return new ArrayList<>(capturedBlockStates.values());
        });
    }

    private void postCaptureBlockStatesFolia(SERVER_LEVEL serverLevel) {
        runRethrowing(() -> {
            Object currentWorldData = GET_CURRENT_WORLD_DATA.invoke(serverLevel);
            CAPTURE_TREE_GENERATION.set(currentWorldData, false);
            CAPTURE_BLOCK_STATES.set(currentWorldData, false);
            var capturedBlockStates = (Map<?, ?>) CAPTURED_BLOCK_STATES.invoke(currentWorldData);
            capturedBlockStates.clear();
        });
    }

    protected abstract void preCaptureStates(SERVER_LEVEL serverLevel);

    protected abstract List<BlockState> getCapturedBlockStatesCopy(SERVER_LEVEL serverLevel);

    protected abstract void postCaptureBlockStates(SERVER_LEVEL serverLevel);

    protected abstract SERVER_LEVEL getServerLevel(World world);

}
