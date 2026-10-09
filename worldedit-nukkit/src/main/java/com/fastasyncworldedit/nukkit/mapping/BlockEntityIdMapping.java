package com.fastasyncworldedit.nukkit.mapping;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Java Edition block-entity identifiers (e.g. {@code minecraft:chest}) to the PascalCase save
 * IDs that Nukkit's {@code BlockEntity.createBlockEntity} expects (e.g. {@code Chest}).
 * <p>
 * Nukkit stores block-entity ids in PascalCase without a namespace prefix ({@code Chest},
 * {@code Sign}, {@code EnchantTable}). When FAWE pastes a Java Edition schematic, the tile NBT
 * carries the JE id ({@code minecraft:chest}). This class normalizes those ids so they resolve
 * against Nukkit's {@code knownBlockEntities} registry.
 * <p>
 * The mapping is explicit for ids that don't follow a simple naming convention
 * (e.g. {@code minecraft:enchanting_table} → {@code EnchantTable}, not {@code EnchantingTable}).
 */
public final class BlockEntityIdMapping {

    private static final Map<String, String> JE_TO_NUKKUK = new HashMap<>();

    static {
        // Explicit mappings for ids that differ from snake_case-to-PascalCase.
        JE_TO_NUKKUK.put("minecraft:enchanting_table", "EnchantTable");
        JE_TO_NUKKUK.put("minecraft:mob_spawner", "MobSpawner");
        JE_TO_NUKKUK.put("minecraft:spawner", "MobSpawner");
        JE_TO_NUKKUK.put("minecraft:daylight_detector", "DaylightDetector");
        JE_TO_NUKKUK.put("minecraft:flower_pot", "FlowerPot");
        JE_TO_NUKKUK.put("minecraft:brewing_stand", "BrewingStand");
        JE_TO_NUKKUK.put("minecraft:blast_furnace", "BlastFurnace");
        JE_TO_NUKKUK.put("minecraft:ender_chest", "EnderChest");
        JE_TO_NUKKUK.put("minecraft:item_frame", "ItemFrame");
        JE_TO_NUKKUK.put("minecraft:glow_item_frame", "GlowItemFrame");
        JE_TO_NUKKUK.put("minecraft:shulker_box", "ShulkerBox");
        JE_TO_NUKKUK.put("minecraft:moving_piston", "MovingBlock");
        JE_TO_NUKKUK.put("minecraft:piston", "PistonArm");
        JE_TO_NUKKUK.put("minecraft:chiseled_bookshelf", "ChiseledBookshelf");
        JE_TO_NUKKUK.put("minecraft:hanging_sign", "HangingSign");
        JE_TO_NUKKUK.put("minecraft:decorated_pot", "DecoratedPot");
        JE_TO_NUKKUK.put("minecraft:creaking_heart", "CreakingHeart");
        JE_TO_NUKKUK.put("minecraft:brushable_block", "BrushableBlock");
        JE_TO_NUKKUK.put("minecraft:suspicious_sand", "BrushableBlock");
        JE_TO_NUKKUK.put("minecraft:suspicious_gravel", "BrushableBlock");
        JE_TO_NUKKUK.put("minecraft:comparator", "Comparator");
        JE_TO_NUKKUK.put("minecraft:beehive", "Beehive");
    }

    private BlockEntityIdMapping() {
    }

    /**
     * Normalize a block-entity id from any known format to Nukkit's PascalCase save id.
     * <p>
     * Handles:
     * <ul>
     *   <li>JE format: {@code minecraft:chest} → {@code Chest}</li>
     *   <li>BE network format: {@code BlockEntityChest} → {@code Chest}</li>
     *   <li>Already-normal Nukkit format: {@code Chest} → {@code Chest}</li>
     * </ul>
     *
     * @param id the raw id from tile NBT
     * @return the normalized id, or the original if no transformation applies
     */
    public static String normalize(String id) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        // Strip BE network prefix "BlockEntity" → e.g. "BlockEntityChest" → "Chest"
        String stripped = id.replaceFirst("BlockEntity", "");
        // Check explicit JE mapping first (handles non-conventional names).
        String explicit = JE_TO_NUKKUK.get(stripped.toLowerCase(Locale.ROOT));
        if (explicit != null) {
            return explicit;
        }
        // JE namespace format: "minecraft:chest" → "Chest" (snake_case to PascalCase).
        if (stripped.contains(":")) {
            String name = stripped.substring(stripped.lastIndexOf(':') + 1);
            return snakeToPascal(name);
        }
        return stripped;
    }

    private static String snakeToPascal(String snake) {
        if (snake == null || snake.isEmpty()) {
            return snake;
        }
        StringBuilder sb = new StringBuilder(snake.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < snake.length(); i++) {
            char c = snake.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
