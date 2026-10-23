package com.fastasyncworldedit.nukkit.util;

import com.sk89q.worldedit.util.TreeGenerator;

/**
 * Maps WorldEdit {@link TreeGenerator.TreeType} values to the tree kinds placeable on Nukkit.
 * <p>
 * Placement happens through two surfaces, both owned by the fork adapters:
 * <ul>
 *   <li>Six shapes (oak, spruce, tall spruce, birch, tall birch, jungle) route through the
 *       legacy {@code ObjectTree.growTree(ChunkManager, x, y, z, random, typeCode)} switch.
 *       That switch silently falls back to oak for unknown codes — this class never passes
 *       an unmapped code, so the fallback cannot mask an unsupported request.</li>
 *   <li>All other shapes are placed by instantiating the fork's generator classes directly
 *       (e.g. {@code ObjectDarkOakTree}, {@code ObjectCherryTree}); the classes exist on both
 *       forks but are <b>not</b> reachable through the legacy switch. Placement code lives in
 *       the adapters because the entry points differ per fork and per superclass
 *       ({@code BasicGenerator.generate} vs {@code ObjectTree.placeObject}).</li>
 * </ul>
 * <p>
 * Supported types and approximations (documented, never silent):
 * <ul>
 *   <li>{@code BIG_TREE} → oak (no separate large-oak generator exists)</li>
 *   <li>{@code PINE} → spruce (Bedrock has no separate pine shape; pine normally never
 *       reaches the platform because FAWE generates it through the EditSession)</li>
 *   <li>{@code MEGA_REDWOOD} → tall spruce ({@code ObjectBigSpruceTree} exists but takes
 *       populator-specific constructor arguments with no server-canonical values)</li>
 *   <li>{@code SMALL_JUNGLE}/{@code SHORT_JUNGLE} → jungle</li>
 *   <li>{@code TALL_MANGROVE} → mangrove (the generator randomises height anyway)</li>
 *   <li>{@code PALE_OAK} — Nukkit-MOT only; {@code AZALEA} — NKX only (queried via
 *       {@code NukkitImplAdapter#supportsTree}, which fails loudly on the other fork)</li>
 * </ul>
 * <p>
 * Unsupported on every Nukkit fork (no generator exists): giant mushrooms
 * ({@code RED_MUSHROOM}/{@code BROWN_MUSHROOM}/{@code RANDOM_MUSHROOM}), {@code JUNGLE_BUSH}
 * (placing a full jungle tree for a bush request would be silently wrong), {@code CHORUS_PLANT},
 * {@code PALE_OAK_CREAKING}, and {@code RANDOM} (its pool includes unsupported types; use a
 * specific type or a supported family random such as {@code randredwood}). These throw
 * {@link UnsupportedOperationException} from {@link #resolve} rather than substituting oak.
 */
public final class NukkitTreeTypes {

    /**
     * Human-readable list of the tree types placeable on Nukkit; used in error messages for
     * unsupported requests so users get an actionable message.
     */
    public static final String SUPPORTED_TYPES =
            "oak (incl. large oak), spruce (incl. pine), tall spruce (incl. large spruce), birch (incl. tall), "
                    + "jungle (incl. small/short), dark oak, acacia, swamp, mangrove (incl. tall), cherry, "
                    + "crimson fungus, warped fungus, pale oak (Nukkit-MOT only), azalea (NKX only)";

    private NukkitTreeTypes() {
    }

    /**
     * A tree shape placeable by a Nukkit fork. {@link #getLegacyCode()} is the
     * {@code ObjectTree.growTree} type code for shapes routed through that legacy entry
     * point, or {@code -1} for kinds the adapters place by instantiating a fork-specific
     * generator class directly.
     */
    public enum NukkitTreeKind {

        OAK(0),
        SPRUCE(1),
        TALL_SPRUCE(101),
        BIRCH(2),
        TALL_BIRCH(10),
        JUNGLE(3),
        DARK_OAK(-1),
        ACACIA(-1),
        SWAMP(-1),
        CHERRY(-1),
        MANGROVE(-1),
        CRIMSON(-1),
        WARPED(-1),
        PALE_OAK(-1),
        AZALEA(-1);

        private final int legacyCode;

        NukkitTreeKind(int legacyCode) {
            this.legacyCode = legacyCode;
        }

        /**
         * The {@code ObjectTree.growTree} code for this kind, or {@code -1} if the kind is
         * placed via a fork-specific generator class instead of the legacy switch.
         */
        public int getLegacyCode() {
            return legacyCode;
        }
    }

    /**
     * Resolve a WorldEdit tree type to a placeable Nukkit tree kind.
     *
     * @param type the WorldEdit tree type
     * @return the placement kind, never {@code null}
     * @throws UnsupportedOperationException if no Nukkit generator exists for the type
     */
    public static NukkitTreeKind resolve(TreeGenerator.TreeType type) {
        return switch (type) {
            case TREE, BIG_TREE -> NukkitTreeKind.OAK;
            case REDWOOD, RANDOM_REDWOOD, PINE -> NukkitTreeKind.SPRUCE;
            case TALL_REDWOOD, MEGA_REDWOOD -> NukkitTreeKind.TALL_SPRUCE;
            case BIRCH, RANDOM_BIRCH -> NukkitTreeKind.BIRCH;
            case TALL_BIRCH -> NukkitTreeKind.TALL_BIRCH;
            case JUNGLE, SMALL_JUNGLE, SHORT_JUNGLE, RANDOM_JUNGLE -> NukkitTreeKind.JUNGLE;
            case SWAMP -> NukkitTreeKind.SWAMP;
            case ACACIA -> NukkitTreeKind.ACACIA;
            case AZALEA -> NukkitTreeKind.AZALEA;
            case DARK_OAK -> NukkitTreeKind.DARK_OAK;
            case CRIMSON_FUNGUS -> NukkitTreeKind.CRIMSON;
            case WARPED_FUNGUS -> NukkitTreeKind.WARPED;
            case MANGROVE, TALL_MANGROVE -> NukkitTreeKind.MANGROVE;
            case CHERRY -> NukkitTreeKind.CHERRY;
            case PALE_OAK -> NukkitTreeKind.PALE_OAK;
            default -> throw unsupported(type);
        };
    }

    /**
     * Whether the given WorldEdit tree type has a Nukkit generator (i.e. {@link #resolve}
     * succeeds). Note that fork-level availability (pale oak on MOT, azalea on NKX) is a
     * separate check on {@code NukkitImplAdapter#supportsTree}.
     */
    public static boolean isSupported(TreeGenerator.TreeType type) {
        try {
            resolve(type);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private static UnsupportedOperationException unsupported(TreeGenerator.TreeType type) {
        String message = "Tree type '" + type.name() + "' is not supported on Nukkit: this Bedrock platform "
                + "provides no generator for it. Supported types: " + SUPPORTED_TYPES + ".";
        if (type == TreeGenerator.TreeType.RANDOM) {
            message += " The random selector can pick types unsupported on this platform; use a specific type "
                    + "or a supported family random (randredwood, randbirch, randjungle).";
        }
        return new UnsupportedOperationException(message);
    }

}
