package com.fastasyncworldedit.bukkit.adapter;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final MethodHandle GET_ENTITIES = createEntitiesGetter();

    @SuppressWarnings("unchecked")
    public static List<Entity> getEntities(World world, Region region) {
        if (GET_ENTITIES == null) {
            return List.of();
        }
        try {
            return (List<Entity>) GET_ENTITIES.invoke(world, region);
        } catch (Throwable t) {
            LOGGER.error("Failed to retrieve entities on Folia", t);
            return List.of();
        }
    }

    // @formatter:off
    private static MethodHandle createEntitiesGetter() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            Class<?> serverLevel = Class.forName("net.minecraft.server.level.ServerLevel");
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Class<?> nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> nmsBlockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Class<?> entityLookup = Class.forName("ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup");

            MethodHandle getEntityLookup;
            try {
                getEntityLookup = lookup.findVirtual(serverLevel, "moonrise$getEntityLookup", methodType(entityLookup));
            } catch (NoSuchMethodException e) {
                getEntityLookup = lookup.findVirtual(serverLevel, "getEntityLookup", methodType(entityLookup));
            }

            MethodHandle getHandle = lookup.findVirtual(craftWorldClass, "getHandle", methodType(serverLevel));
            MethodHandle getAll = lookup.findVirtual(entityLookup, "getAll", methodType(Iterable.class));
            MethodHandle getEntities = filterReturnValue(filterReturnValue(getHandle, getEntityLookup), getAll);
            MethodHandle regionContainsXYZ = lookup.findVirtual(Region.class, "contains", methodType(boolean.class, int.class, int.class, int.class));
            MethodHandle blockPos = lookup.findVirtual(nmsEntityClass, "blockPosition", methodType(nmsBlockPosClass));
            MethodHandle getX = lookup.findVirtual(nmsBlockPosClass, "getX", methodType(int.class));
            MethodHandle getY = lookup.findVirtual(nmsBlockPosClass, "getY", methodType(int.class));
            MethodHandle getZ = lookup.findVirtual(nmsBlockPosClass, "getZ", methodType(int.class));

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
            LOGGER.error("Failed to create Folia entities getter", t);
            return null;
        }
    }
    // @formatter:on
}
