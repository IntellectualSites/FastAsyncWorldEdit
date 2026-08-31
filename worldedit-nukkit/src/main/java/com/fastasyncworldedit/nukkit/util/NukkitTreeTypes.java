package com.fastasyncworldedit.nukkit.util;

import com.sk89q.worldedit.util.TreeGenerator;

/**
 * Maps WorldEdit {@link TreeGenerator.TreeType} values to the integer type codes understood by
 * Nukkit's {@code ObjectTree.growTree} on the NKX/MOT forks.
 * <p>
 * Nukkit's {@code ObjectTree} only switches on six codes (0=oak, 1=spruce, 2=birch,
 * 3=jungle, 10=tall birch, 101=tall spruce); anything else falls back to oak (0). More exotic
 * WorldEdit types (dark oak, acacia, chorus, nether fungi, mangrove, cherry, pale oak) have no
 * direct {@code ObjectTree.growTree} equivalent and also fall back to oak. Callers may detect
 * unmapped types via {@link #isMapped(TreeGenerator.TreeType)} if they need to skip placement
 * rather than substitute.
 */
public final class NukkitTreeTypes {

    /** Oak / default. */
    public static final int OAK = 0;
    public static final int SPRUCE = 1;
    public static final int BIRCH = 2;
    public static final int JUNGLE = 3;
    public static final int TALL_BIRCH = 10;
    public static final int TALL_SPRUCE = 101;

    private NukkitTreeTypes() {
    }

    /**
     * Translate a WorldEdit tree type to the Nukkit {@code ObjectTree} code, falling back to
     * {@link #OAK} when no closer match exists.
     */
    public static int toNukkitCode(TreeGenerator.TreeType type) {
        if (type == null) {
            return OAK;
        }
        switch (type) {
            case TREE, BIG_TREE -> {
                return OAK;
            }
            case REDWOOD, RANDOM_REDWOOD -> {
                return SPRUCE;
            }
            case TALL_REDWOOD -> {
                return TALL_SPRUCE;
            }
            case MEGA_REDWOOD -> {
                return TALL_SPRUCE;
            }
            case BIRCH, RANDOM_BIRCH -> {
                return BIRCH;
            }
            case TALL_BIRCH -> {
                return TALL_BIRCH;
            }
            case JUNGLE, SMALL_JUNGLE, SHORT_JUNGLE, RANDOM_JUNGLE, JUNGLE_BUSH -> {
                return JUNGLE;
            }
            default -> {
                return OAK;
            }
        }
    }

    /**
     * Whether {@code type} has a non-oak Nukkit equivalent. Used to decide whether a placement
     * should be attempted or skipped when an exact match is required.
     */
    public static boolean isMapped(TreeGenerator.TreeType type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case TREE, BIG_TREE, REDWOOD, RANDOM_REDWOOD, TALL_REDWOOD, MEGA_REDWOOD,
                    BIRCH, RANDOM_BIRCH, TALL_BIRCH,
                    JUNGLE, SMALL_JUNGLE, SHORT_JUNGLE, RANDOM_JUNGLE, JUNGLE_BUSH -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

}
