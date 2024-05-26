package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_20_R3;

import com.fastasyncworldedit.core.extent.processor.PlacementStateProcessor;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;

import javax.annotation.Nullable;

public class PaperweightPlacementStateProcessor extends PlacementStateProcessor {

    private final PaperweightFaweAdapter adapter = ((PaperweightFaweAdapter) WorldEditPlugin
            .getInstance()
            .getBukkitImplAdapter());
    private final FaweMutableBlockPlaceContext mutableBlockPlaceContext;

    public PaperweightPlacementStateProcessor(
            final Extent extent,
            final BlockTypeMask mask,
            final boolean includeUnedited
    ) {
        super(extent, mask, includeUnedited);
        World world;
        if (extent.isWorld()) {
            world = (World) extent;
        } else if (extent instanceof EditSession session) {
            world = session.getWorld();
        } else if ((world = new ExtentTraverser<>(extent).findAndGet(BukkitWorld.class)) == null) {
            throw new UnsupportedOperationException("Cannot find world of extent.");
        }
        BukkitWorld bukkitWorld;
        if (world instanceof WorldWrapper wrapper) {
            bukkitWorld = (BukkitWorld) wrapper.getParent();
        } else {
            bukkitWorld = (BukkitWorld) world;
        }
        PaperweightLevelProxy proxyLevel = PaperweightLevelProxy.getInstance(
                ((CraftWorld) bukkitWorld.getWorld()).getHandle(),
                extent
        );
        mutableBlockPlaceContext = new FaweMutableBlockPlaceContext(proxyLevel);
        proxyLevel.setEnabled(true);
    }

    @Override
    protected char getStateAtFor(
            int x,
            int y,
            int z,
            BlockState state,
            Vector3 clickPos,
            Direction clickedFaceDirection,
            BlockVector3 clickedBlock
    ) {
        Block block = ((PaperweightBlockMaterial) state.getMaterial()).getBlock();
        Vec3 pos = new Vec3(clickPos.x(), clickPos.y(), clickPos.z());
        net.minecraft.core.Direction side = net.minecraft.core.Direction.valueOf(clickedFaceDirection.toString());
        BlockPos blockPos = new BlockPos(clickedBlock.x(), clickedBlock.y(), clickedBlock.z());
        net.minecraft.world.level.block.state.BlockState newState = block.getStateForPlacement(mutableBlockPlaceContext.withSetting(
                new BlockHitResult(pos, side, blockPos, false),
                side.getOpposite()
        ));
        return newState == null ? BlockTypesCache.ReservedIDs.AIR :
                adapter.ibdIDToOrdinal(Block.BLOCK_STATE_REGISTRY.getId(newState));
    }

    @Override
    @Nullable
    public Extent construct(Extent child) {
        if (child == getExtent()) {
            return this;
        }
        return new PaperweightPlacementStateProcessor(child, mask, includeUnedited);
    }

    @Override
    public PlacementStateProcessor fork() {
        return new PaperweightPlacementStateProcessor(extent, mask, includeUnedited);
    }

}
