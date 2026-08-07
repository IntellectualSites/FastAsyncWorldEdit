package com.fastasyncworldedit.nukkit.adapter;

/**
 * Granular runtime capabilities reported by the active Nukkit adapter.
 * <p>
 * Nukkit forks vary widely in feature support. Rather than branching on
 * platform name strings, FAWE queries capability flags to determine what
 * operations are safe. Most capabilities are absent because Nukkit's
 * Bedrock-based architecture lacks Java Edition features like CUI
 * protocol support, explicit relighting, and fake chunk packets.
 * <p>
 * Key differences from Bukkit:
 * <ul>
 *   <li>Bukkit generally supports all capabilities; Nukkit supports very few</li>
 *   <li>Chunk sections and chunk caching are not real abstractions on Nukkit</li>
 *   <li>Tree history capture is unsupported because Nukkit's tree API mutates directly</li>
 * </ul>
 *
 * @see NukkitImplAdapter#getCapabilities()
 */
public enum NukkitPlatformCapabilities {

    FAKE_CHUNKS("Can send fake chunk packets for clipboard previews."),
    EXPLICIT_RELIGHTING("Can manually recalculate lighting instead of relying on Nukkit's internal lighting updates."),
    CUI_SUPPORT("Can communicate with the WorldEdit CUI protocol."),
    CHUNK_SECTIONS("Provides a real chunk section abstraction for direct section-level access."),
    CHUNK_CACHING("Allows chunk references to be cached safely across queue operations."),
    THREE_DIMENSIONAL_BIOMES("Stores biomes with a vertical coordinate instead of legacy 2D x/z columns.");

    private final String description;

    NukkitPlatformCapabilities(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
