package com.fastasyncworldedit.bukkit.adapter;

import com.destroystokyo.paper.util.SneakyThrow;
import com.fastasyncworldedit.bukkit.util.BukkitReflectionUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.adapter.Refraction;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.dropReturn;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.iteratedLoop;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;

public class BukkitFoliaAdapter {

    private static final MethodHandle GET_ENTITIES = createEntitiesGetter();

    @SuppressWarnings("unchecked")
    public static List<Entity> getEntities(World world, Region region) {
        try {
            return (List<Entity>) GET_ENTITIES.invoke(world, region);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // @formatter:off
    private static MethodHandle createEntitiesGetter() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> craftWorldClass = BukkitReflectionUtils.getCbClass("CraftWorld");
            Class<?> serverLevel = Class.forName(Refraction.pickName("net.minecraft.server.level.ServerLevel", "net.minecraft.server.level.WorldServer"));
            Class<?> craftEntity = BukkitReflectionUtils.getCbClass("entity.CraftEntity");
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> nmsBlockPosClass = Class.forName(Refraction.pickName("net.minecraft.core.BlockPos", "net.minecraft.core.BlockPosition"));
            Class<?> entityLookup = Class.forName("io.papermc.paper.chunk.system.entity.EntityLookup");
            MethodHandle getHandle = lookup.findVirtual(craftWorldClass, "getHandle", methodType(serverLevel));
            MethodHandle getEntityLookup = lookup.findVirtual(serverLevel, "getEntityLookup", methodType(entityLookup));
            MethodHandle getAll = lookup.findVirtual(entityLookup, Refraction.pickName("getAll", "a"), methodType(Iterable.class));
            MethodHandle getEntities = filterReturnValue(filterReturnValue(getHandle, getEntityLookup), getAll);
            MethodHandle regionContainsXYZ = lookup.findVirtual(Region.class, "contains", methodType(boolean.class, int.class, int.class, int.class));
            MethodHandle blockPos = lookup.findVirtual(nmsEntityClass, Refraction.pickName("blockPosition", "dm"), methodType(nmsBlockPosClass));
            MethodHandle getX = lookup.findVirtual(nmsBlockPosClass, Refraction.pickName("getX", "u"), methodType(int.class));
            MethodHandle getY = lookup.findVirtual(nmsBlockPosClass, Refraction.pickName("getY", "v"), methodType(int.class));
            MethodHandle getZ = lookup.findVirtual(nmsBlockPosClass, Refraction.pickName("getZ", "w"), methodType(int.class));
            MethodHandle regionContainsBPBPBP = filterArguments(regionContainsXYZ, 1, getX, getY, getZ);
            MethodHandle regionContainsBlockPos = permuteArguments(regionContainsBPBPBP, methodType(boolean.class, nmsBlockPosClass, Region.class), 1, 0, 0, 0);
            MethodHandle isInRegion = dropArguments(filterArguments(regionContainsBlockPos, 0, blockPos), 0, ArrayList.class);
            MethodHandle getBukkitEntity = lookup.findVirtual(nmsEntityClass, "getBukkitEntity", methodType(craftEntity));
            MethodHandle adapt = lookup.findStatic(BukkitAdapter.class, "adapt", methodType(Entity.class, org.bukkit.entity.Entity.class));
            MethodHandle weEntity = filterReturnValue(getBukkitEntity.asType(getBukkitEntity.type().changeReturnType(org.bukkit.entity.Entity.class)), adapt);
            MethodHandle add = lookup.findVirtual(ArrayList.class, "add", methodType(boolean.class, Object.class));
            MethodHandle addConverted = filterArguments(add, 1, weEntity.asType(weEntity.type().changeReturnType(Object.class)));
            MethodHandle arrayListIdentity = MethodHandles.identity(ArrayList.class);
            MethodHandle addConvertedReturn = collectArguments(dropArguments(arrayListIdentity, 1, nmsEntityClass), 0, dropReturn(addConverted));
            MethodHandle addConvertedReturnCollapsed = permuteArguments(addConvertedReturn, methodType(ArrayList.class, ArrayList.class, nmsEntityClass), 0, 1, 0, 1);
            MethodHandle newArrayListHandle = lookup.findConstructor(ArrayList.class, methodType(void.class));
            MethodHandle ifTrue = dropArguments(addConvertedReturnCollapsed, 2, Region.class);
            MethodHandle ifFalse = dropArguments(arrayListIdentity, 1, nmsEntityClass, Region.class);
            MethodHandle ifInRegion = guardWithTest(isInRegion, ifTrue, ifFalse);
            MethodHandle iterate = iteratedLoop(null, newArrayListHandle, dropArguments(ifInRegion, 2, Iterable.class));
            return filterArguments(iterate, 0, getEntities);
        } catch (Throwable t) {
            SneakyThrow.sneaky(t);
            return null;
        }
    }
    // @formatter:on
}
