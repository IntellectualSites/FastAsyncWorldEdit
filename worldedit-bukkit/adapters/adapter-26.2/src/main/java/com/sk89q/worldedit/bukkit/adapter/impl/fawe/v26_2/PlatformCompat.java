package com.sk89q.worldedit.bukkit.adapter.impl.fawe.v26_2;

import com.fastasyncworldedit.bukkit.util.PaperSupport;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class PlatformCompat {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static MethodHandle SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA;

    static {
        if (!PaperSupport.isPaper()) {
            try {
                //noinspection JavaLangInvokeHandleSignature (not available on Paper)
                SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA = MethodHandles.lookup().findStatic(
                        CraftBlockData.class,
                        "fromData",
                        MethodType.methodType(CraftBlockData.class, net.minecraft.world.level.block.state.BlockState.class)
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                LOGGER.error("Failed to lookup CraftBlockData#fromData(BlockState)", e);
            }
        }
    }

    public static CraftBlockData fromData(BlockState state) throws Throwable {
        if (SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA == null) {
            return state.asBlockData();
        }
        return (CraftBlockData) SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA.invokeExact(state);
    }

    public static CraftBlockData fromDataUnsafe(BlockState state) {
        try {
            return fromData(state);
        } catch (Throwable e) {
            LOGGER.error("Unsafe call to #fromData", e);
        }
        return null; // this will most likely fail somewhere along the call (NPE)
    }

}
