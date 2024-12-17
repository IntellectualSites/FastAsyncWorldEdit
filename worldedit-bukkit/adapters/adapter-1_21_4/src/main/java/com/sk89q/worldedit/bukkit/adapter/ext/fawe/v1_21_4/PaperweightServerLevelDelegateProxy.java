/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_21_4;

import com.fastasyncworldedit.core.internal.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.bukkit.adapter.impl.fawe.v1_21_4.PaperweightFaweAdapter;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

public class PaperweightServerLevelDelegateProxy implements InvocationHandler {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    // FAWE start - extent not EditSession
    private final Extent editSession;
    //FAWE end
    private final ServerLevel serverLevel;
    //FAWE start - use FAWE adapter
    private final PaperweightFaweAdapter adapter = ((PaperweightFaweAdapter) WorldEditPlugin
            .getInstance()
            .getBukkitImplAdapter());
    //FAWE end
    //FAWE start - force error if method not caught by this instance
    private final boolean errorOnPassthrough;
    //FAWE end

    private PaperweightServerLevelDelegateProxy(EditSession editSession, ServerLevel serverLevel, PaperweightAdapter adapter) {
        this.editSession = editSession;
        this.serverLevel = serverLevel;
        //FAWE start
        this.errorOnPassthrough = false;
        //FAWE end
    }

    public static WorldGenLevel newInstance(EditSession editSession, ServerLevel serverLevel, PaperweightAdapter adapter) {
        return (WorldGenLevel) Proxy.newProxyInstance(
                serverLevel.getClass().getClassLoader(),
                serverLevel.getClass().getInterfaces(),
                new PaperweightServerLevelDelegateProxy(editSession, serverLevel, adapter)
        );
    }

    //FAWE start - force error if method not caught by this instance
    private PaperweightServerLevelDelegateProxy(Extent extent, ServerLevel serverLevel, boolean errorOnPassthrough) {
        this.editSession = extent;
        this.serverLevel = serverLevel;
        this.errorOnPassthrough = errorOnPassthrough;
    }

    public static WorldGenLevel newInstance(Extent extent, ServerLevel serverLevel, boolean errorOnPassthrough) {
        return (WorldGenLevel) Proxy.newProxyInstance(
                serverLevel.getClass().getClassLoader(),
                serverLevel.getClass().getInterfaces(),
                new PaperweightServerLevelDelegateProxy(extent, serverLevel, errorOnPassthrough)
        );
    }
    //FAWE end

    @Nullable
    private BlockEntity getBlockEntity(BlockPos blockPos) {
        BlockEntity tileEntity = this.serverLevel.getChunkAt(blockPos).getBlockEntity(blockPos);
        if (tileEntity == null) {
            return null;
        }
        BlockEntity newEntity = tileEntity.getType().create(blockPos, getBlockState(blockPos));
        newEntity.loadWithComponents(
                (CompoundTag) adapter.fromNativeLin(this.editSession.getFullBlock(
                        blockPos.getX(),
                        blockPos.getY(),
                        blockPos.getZ()
                ).getNbtReference().getValue()),
                this.serverLevel.registryAccess()
        );

        return newEntity;
    }

    private BlockState getBlockState(BlockPos blockPos) {
        return adapter.adapt(this.editSession.getBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    private boolean setBlock(BlockPos blockPos, BlockState blockState) {
        try {
            return editSession.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), adapter.adapt(blockState));
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean removeBlock(BlockPos blockPos, boolean bl) {
        try {
            return editSession.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), BlockTypes.AIR.getDefaultState());
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }
    }

    private FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    private boolean isWaterAt(BlockPos pos) {
        return getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //FAWE start - cannot use switch where method names are equal
        String methodName = method.getName();
        if (Refraction.pickName("getBlockState", "a_").equals(methodName)) {
            if (args.length == 1 && args[0] instanceof BlockPos blockPos) {
                // getBlockState
                return getBlockState(blockPos);
            }
        }
        if (Refraction.pickName("getBlockEntity", "c_").equals(methodName)) {
            if (args.length == 1 && args[0] instanceof BlockPos blockPos) {
                // getBlockEntity
                return getBlockEntity(blockPos);
            }
        }
        if ("a".equals(methodName) || "setBlock".equals(methodName) || "removeBlock".equals(methodName) || "destroyBlock".equals(
                methodName)) {
            if (args.length >= 2 && args[0] instanceof BlockPos blockPos && args[1] instanceof BlockState blockState) {
                // setBlock
                return setBlock(blockPos, blockState);
            } else if (args.length >= 2 && args[0] instanceof BlockPos blockPos && args[1] instanceof Boolean bl) {
                // removeBlock (and also matches destroyBlock)
                return removeBlock(blockPos, bl);
            }
        }
        //FAWE start
        if (Refraction.pickName("getFluidState", "b_").equals(methodName)) { //net.minecraft.world.level.BlockGetter
            if (args.length == 1 && args[0] instanceof BlockPos blockPos) {
                return getFluidState(blockPos);
            }
        }
        if (Refraction.pickName("isWaterAt", "z").equals(methodName)) { //net.minecraft.world.level.LevelReader
            if (args.length == 1 && args[0] instanceof BlockPos blockPos) {
                return isWaterAt(blockPos);
            }
        }
        if (Refraction.pickName("getEntities", "a_").equals(methodName)) { //net.minecraft.world.level.EntityGetter
            if (args.length == 2 && args[0] instanceof Entity && args[1] instanceof AABB) {
                return new ArrayList<>();
            }
        }
        // Specific passthroughs that we want to allow
        // net.minecraft.world.level.BlockAndTintGetter
        if (Refraction.pickName("getRawBrightness", "b").equals(methodName)) {
            return method.invoke(this.serverLevel, args);
        }
        // net.minecraft.world.level.LevelHeightAccessor
        if (Refraction.pickName("getMaxBuildHeight", "al").equals(methodName)) {
            if (args.length == 0) {
                return method.invoke(this.serverLevel, args);
            }
        }
        // net.minecraft.world.level.SignalGetter
        if (Refraction.pickName("hasNeighborSignal", "C").equals(methodName)) {
            if (args.length == 1 && args[0] instanceof BlockPos) {
                return method.invoke(this.serverLevel, args);
            }
        }
        if (Refraction.pickName("getSignal", "c").equals(methodName)) {
            if (args.length == 2 && args[0] instanceof BlockPos && args[1] instanceof Direction) {
                return method.invoke(this.serverLevel, args);
            }
        }
        if (Refraction.pickName("getControlInputSignal", "a").equals(methodName)) {
            if (args.length == 3 && args[0] instanceof BlockPos && args[1] instanceof Direction && args[2] instanceof Boolean) {
                return method.invoke(this.serverLevel, args);
            }
        }
        if (Refraction.pickName("getDirectSignal", "a").equals(methodName)) {
            if (args.length == 2 && args[0] instanceof BlockPos && args[1] instanceof Direction) {
                return method.invoke(this.serverLevel, args);
            }
        }
        //FAWE start - force error if method not caught by this instance
        if (errorOnPassthrough) {
            LOGGER.error(
                    """
                            Attempted passthough of method {}.
                            Method argument types: {}
                            Method argument values: {}
                            """,
                    method.getName(),
                    Arrays.stream(args).map(a -> a.getClass().getName()).toList(),
                    Arrays.stream(args).map(Object::toString).toList()
            );
            throw new FaweException("Method required passthrough.");
        }
        //FAWE end

        return method.invoke(this.serverLevel, args);
    }

}
