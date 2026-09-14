package com.fastasyncworldedit.forge1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

@Mod(modid = FaweForge1710Mod.MOD_ID, name = "FastAsyncWorldEdit", version = "0.0.1-S0", acceptableRemoteVersions = "*")
public class FaweForge1710Mod {

    public static final String MOD_ID = "fastasyncworldedit";

    private Logger logger;

    /**
     * S0 feasibility probe: exercises Java 21 language/API features that FAWE core relies on
     * (records, pattern-matching switch, virtual threads) plus a reobfuscated vanilla reference.
     */
    private record Probe(String name, int value) {
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        Object probe = new Probe("java", Runtime.version().feature());
        String described = switch (probe) {
            case Probe(String name, int value) when value >= 21 -> name + " " + value + " (ok)";
            case Probe p -> p.name() + " " + p.value() + " (too old)";
            default -> "unknown";
        };
        logger.info("[FAWE-S0] preInit: runtime {}", described);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) throws InterruptedException {
        Thread virtual = Thread.ofVirtual().name("fawe-s0-probe").start(() -> {
            Block stone = Block.getBlockById(1);
            logger.info("[FAWE-S0] virtual thread {} sees block 1 = {} ({})",
                    Thread.currentThread().isVirtual(), Block.blockRegistry.getNameForObject(stone), stone.getUnlocalizedName());
        });
        virtual.join();
        logger.info("[FAWE-S0] server started, worlds loaded: {}", MinecraftServer.getServer().worldServers.length);
    }

}
