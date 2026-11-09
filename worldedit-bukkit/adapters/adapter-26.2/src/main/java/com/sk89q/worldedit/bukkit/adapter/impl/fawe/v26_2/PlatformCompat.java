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

    private static final MethodHandle SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA;

    static {
        MethodHandle tmpSpigotCraftBlockDataFromData = null;
        if (!PaperSupport.isPaper()) {
            try {
                //noinspection JavaLangInvokeHandleSignature (not available on Paper)
                tmpSpigotCraftBlockDataFromData = MethodHandles.lookup().findStatic(
                        CraftBlockData.class,
                        "fromData",
                        MethodType.methodType(CraftBlockData.class, BlockState.class)
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                LOGGER.error("Failed to lookup CraftBlockData#fromData(BlockState)", e);
            }
        }
        SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA = tmpSpigotCraftBlockDataFromData;
    }

    /**
     * Adapts a {@link BlockState} to a {@link CraftBlockData} using platform-specific methods.
     * On Paper {@code BlockState#asBlockData()} is used, on Spigot {@code CraftBlockData#fromData(BlockState)} is used.
     * <p>
     * Uses reflection on Spigot, as Paper dropped the Spigot method (-> Can't be directly called).
     *
     * @param state the {@link BlockState} to adapt.
     * @return the {@link CraftBlockData}
     */
    public static CraftBlockData fromData(BlockState state) {
        if (SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA == null) {
            return state.asBlockData();
        }
        try {
            return (CraftBlockData) SPIGOT__CRAFT_BLOCK_DATA__FROM_DATA.invokeExact(state);
        } catch (Throwable throwable) {
            throw new RuntimeException("Caught unexpected Exception while converting BlockState to CraftBlockData", throwable);
        }
    }

}
