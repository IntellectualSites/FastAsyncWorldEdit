package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_5;

import com.fastasyncworldedit.core.extent.processor.PlacementStateProcessor;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.wrappers.WorldWrapper;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftWorld;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaperweightPlacementStateProcessor extends PlacementStateProcessor {

    private final PaperweightFaweAdapter adapter = ((PaperweightFaweAdapter) WorldEditPlugin
            .getInstance()
            .getBukkitImplAdapter());
    private final PaperweightFaweMutableBlockPlaceContext mutableBlockPlaceContext;
    private final PaperweightLevelProxy proxyLevel;

    public PaperweightPlacementStateProcessor(Extent extent, BlockTypeMask mask, Region region) {
        super(extent, mask, region);
        World world = ExtentTraverser.getWorldFromExtent(extent);
        if (world == null) {
            throw new UnsupportedOperationException(
                    "World is required for PlacementStateProcessor but none found in given extent.");
        }
        BukkitWorld bukkitWorld;
        if (world instanceof WorldWrapper wrapper) {
            bukkitWorld = (BukkitWorld) wrapper.getParent();
        } else {
            bukkitWorld = (BukkitWorld) world;
        }
        this.proxyLevel = PaperweightLevelProxy.getInstance(((CraftWorld) bukkitWorld.getWorld()).getHandle(), this);
        this.mutableBlockPlaceContext = new PaperweightFaweMutableBlockPlaceContext(proxyLevel);
    }

    private PaperweightPlacementStateProcessor(
            Extent extent,
            BlockTypeMask mask,
            Map<SecondPass, Integer> crossChunkSecondPasses,
            ServerLevel serverLevel,
            ThreadLocal<PlacementStateProcessor> threadProcessors,
            Region region,
            AtomicBoolean finished
    ) {
        super(extent, mask, crossChunkSecondPasses, threadProcessors, region, finished);
        this.proxyLevel = PaperweightLevelProxy.getInstance(serverLevel, this);
        this.mutableBlockPlaceContext = new PaperweightFaweMutableBlockPlaceContext(proxyLevel);
    }

    @Override
    protected int getStateAtFor(
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
        return newState == null ? BlockTypesCache.ReservedIDs.AIR : adapter.ibdIDToOrdinal(Block.BLOCK_STATE_REGISTRY.getId(
                newState));
    }

    @Override
    @Nullable
    public Extent construct(Extent child) {
        if (child == getExtent()) {
            return this;
        }
        return new PaperweightPlacementStateProcessor(child, mask, region);
    }

    @Override
    public PlacementStateProcessor fork() {
        return new PaperweightPlacementStateProcessor(
                extent,
                mask,
                postCompleteSecondPasses,
                proxyLevel.serverLevel,
                threadProcessors,
                region,
                finished
        );
    }

}
