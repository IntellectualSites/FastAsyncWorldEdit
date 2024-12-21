package com.fastasyncworldedit.bukkit.adapter;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.TreeGenerator;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.List;
import java.util.Map;

/**
 * A base class for version-specific implementations of the BukkitImplAdapter
 *
 * @param <TAG>          the version-specific NBT tag type
 * @param <SERVER_LEVEL> the version-specific ServerLevel type
 */
public abstract class FaweAdapter<TAG, SERVER_LEVEL> extends CachedBukkitAdapter implements IDelegateBukkitImplAdapter<TAG> {

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

}
