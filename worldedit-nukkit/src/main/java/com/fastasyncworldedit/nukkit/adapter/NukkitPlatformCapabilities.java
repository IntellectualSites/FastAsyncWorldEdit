package com.fastasyncworldedit.nukkit.adapter;

/**
 * Granular runtime capabilities reported by the active Nukkit adapter.
 * <p>
 * Nukkit forks vary in feature support. Rather than branching on platform
 * name strings, code queries capability flags to determine what operations
 * are safe: {@code NukkitGetBlocks} switches its biome write strategy on
 * {@link #THREE_DIMENSIONAL_BIOMES}, and {@code NukkitPlayer} gates CUI
 * dispatch on {@link #CUI_SUPPORT} (never granted today — Bedrock clients
 * have no modded CUI channel).
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit adapters are selected per Minecraft version at compile time and
 *       are broadly capable; Nukkit adapters are selected per fork at runtime
 *       and report few capabilities</li>
 *   <li>Chunk sections and chunk caching are emulated uniformly by
 *       {@code NukkitGetBlocks} for every fork, so they are not fork-dependent
 *       capabilities here</li>
 * </ul>
 * <p>
 * Policy: a capability constant must have at least one runtime query site. Do
 * not add speculative flags for features no adapter reports and no code
 * branches on — unsupported behaviour is instead made loud (a thrown
 * {@code UnsupportedOperationException} with an explanation) at the place it
 * would otherwise fail silently.
 *
 * @see NukkitImplAdapter#getCapabilities()
 */
public enum NukkitPlatformCapabilities {

    CUI_SUPPORT("Can communicate with the WorldEdit CUI protocol."),
    THREE_DIMENSIONAL_BIOMES("Stores biomes with a vertical coordinate instead of legacy 2D x/z columns.");

    private final String description;

    NukkitPlatformCapabilities(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
