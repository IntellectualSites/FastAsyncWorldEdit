package com.fastasyncworldedit.forge1710;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.forge1710.entity.Forge1710Player;
import com.fastasyncworldedit.forge1710.registry.NativeBlockMapper;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.event.platform.SessionIdleEvent;
import com.sk89q.worldedit.util.Location;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(modid = FaweForge1710Mod.MOD_ID, name = "FastAsyncWorldEdit", version = FaweForge1710Mod.VERSION, acceptableRemoteVersions = "*")
public class FaweForge1710Mod {

    public static final String MOD_ID = "fastasyncworldedit";
    public static final String VERSION = "0.1.0-M1";

    @Mod.Instance(MOD_ID)
    public static FaweForge1710Mod instance;

    private Logger logger;
    private Path workingDir;
    private Forge1710Configuration config;
    private Forge1710Platform platform;
    private Forge1710TaskManager taskManager;
    private boolean faweInitialised;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        workingDir = event.getModConfigurationDirectory().toPath().resolve("worldedit");
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        taskManager = new Forge1710TaskManager();
        FMLCommonHandler.instance().bus().register(taskManager);
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        logger.info("FastAsyncWorldEdit for Forge 1.7.10 ({}) loaded on Java {}", VERSION, Runtime.version().feature());
    }

    /**
     * Runs on the server thread, which FAWE records as its main thread.
     */
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        config = new Forge1710Configuration(workingDir);
        config.load();

        platform = new Forge1710Platform(this);
        WorldEdit.getInstance().getPlatformManager().register(platform);
        // Same order as the Bukkit/CLI platforms: "registered" initialises the platform manager (capabilities become
        // queryable), "ready" later enables commands and game hooks.
        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());

        if (!faweInitialised) {
            try {
                Fawe.set(new FaweForge1710(workingDir.toFile(), taskManager));
                Fawe.setupInjector();
                faweInitialised = true;
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialise FAWE", e);
            }
        }

        // Reads the (now frozen) Forge block registry. Must not touch FAWE block types until the platform is ready.
        NativeBlockMapper.get();
        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent(platform));

        if (Boolean.getBoolean("fawe.forge1710.selftest")) {
            event.registerServerCommand(new Forge1710SelfTestCommand());
        }
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        if (platform == null) {
            return;
        }
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        worldEdit.getEventBus().post(new PlatformUnreadyEvent(platform));
        worldEdit.getPlatformManager().unregister(platform);
        platform = null;
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (platform == null || !platform.isHookingEvents() || event.world.isRemote
                || !(event.entityPlayer instanceof EntityPlayerMP playerMP)) {
            return;
        }
        WorldEdit we = WorldEdit.getInstance();
        Forge1710Player player = Forge1710Adapter.adapt(playerMP);
        switch (event.action) {
            case LEFT_CLICK_BLOCK -> {
                Location pos = new Location(Forge1710Adapter.adapt((WorldServer) event.world), event.x, event.y, event.z);
                if (we.handleBlockLeftClick(player, pos, null)) {
                    event.setCanceled(true);
                }
                if (we.handleArmSwing(player)) {
                    event.setCanceled(true);
                }
            }
            case RIGHT_CLICK_BLOCK -> {
                Location pos = new Location(Forge1710Adapter.adapt((WorldServer) event.world), event.x, event.y, event.z);
                if (we.handleBlockRightClick(player, pos, null)) {
                    event.setCanceled(true);
                }
                if (we.handleRightClick(player)) {
                    event.setCanceled(true);
                }
            }
            case RIGHT_CLICK_AIR -> {
                if (we.handleRightClick(player)) {
                    event.setCanceled(true);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            WorldEdit.getInstance().getEventBus().post(new SessionIdleEvent(Forge1710Adapter.adapt(player).getSessionKey()));
        }
    }

    Forge1710Configuration getConfig() {
        return config;
    }

    public File getWorkingDir() {
        return workingDir.toFile();
    }

}
