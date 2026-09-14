package com.fastasyncworldedit.forge1710;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mod(modid = FaweForge1710Mod.MOD_ID, name = "FastAsyncWorldEdit", version = "0.0.1-M0", acceptableRemoteVersions = "*")
public class FaweForge1710Mod {

    public static final String MOD_ID = "fastasyncworldedit";

    private Logger logger;
    private File source;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        source = event.getSourceFile();
        logger.info("[FAWE-M0] preInit on Java {} from {}", Runtime.version().feature(), source);
    }

    /**
     * M0 link check: load (without initialising) every class bundled in the mod jar through the mod class loader,
     * so missing or unrelocated dependencies surface as NoClassDefFoundError before any platform code exists.
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (source == null || !source.isFile()) {
            logger.warn("[FAWE-M0] mod source is not a jar ({}); skipping link check", source);
            return;
        }
        ClassLoader loader = FaweForge1710Mod.class.getClassLoader();
        int loaded = 0;
        int failed = 0;
        Map<String, Integer> missing = new LinkedHashMap<>();
        Map<String, String> firstFailure = new LinkedHashMap<>();
        try (JarFile jar = new JarFile(source)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (!entry.endsWith(".class") || entry.startsWith("META-INF/") || entry.endsWith("module-info.class")) {
                    continue;
                }
                String className = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
                try {
                    Class.forName(className, false, loader);
                    loaded++;
                } catch (Throwable t) {
                    failed++;
                    String key = t.getClass().getSimpleName() + ": " + t.getMessage();
                    missing.merge(key, 1, Integer::sum);
                    firstFailure.putIfAbsent(key, className);
                }
            }
        } catch (IOException e) {
            logger.error("[FAWE-M0] could not read mod jar {}", source, e);
            return;
        }
        logger.info("[FAWE-M0] link check: {} classes loaded, {} failed, {} distinct causes", loaded, failed, missing.size());
        missing.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> logger.info("[FAWE-M0]   {}x {} (e.g. {})", e.getValue(), e.getKey(), firstFailure.get(e.getKey())));
    }

}
