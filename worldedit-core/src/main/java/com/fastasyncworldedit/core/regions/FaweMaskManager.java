package com.fastasyncworldedit.core.regions;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.regions.filter.RegionFilter;
import com.sk89q.worldedit.entity.Player;

import java.util.Locale;

public abstract class FaweMaskManager {

    private final String key;

    public FaweMaskManager(final String plugin) {
        this.key = plugin.toLowerCase(Locale.ROOT);
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }

    /**
     * Get a {@link FaweMask} for the given player and {@link MaskType}
     *
     * @deprecated Use {@link #getMask(Player, MaskType, boolean)}
     */
    @Deprecated(forRemoval = true)
    public FaweMask getMask(final Player player, MaskType type) {
        return getMask(player, type, true);
    }

    /**
     * Get a {@link FaweMask} for the given player and {@link MaskType}. If isWhitelist is false, will return a "blacklist" mask.
     */
    public FaweMask getMask(final Player player, MaskType type, boolean isWhitelist) {
        return getMask(player, type);
    }

    /**
     * @deprecated Not used internally
     */
    @Deprecated(forRemoval = true)
    public boolean isValid(FaweMask mask) {
        return true;
    }

    /**
     * @deprecated Not used internally
     */
    @Deprecated(forRemoval = true)
    public RegionFilter getFilter(String world) {
        return null;
    }

    public boolean isExclusive() {
        return Settings.IMP.REGION_RESTRICTIONS_OPTIONS.EXCLUSIVE_MANAGERS.contains(this.key);
    }

    public enum MaskType {
        OWNER,
        MEMBER;

        public static MaskType getDefaultMaskType() {
            try {
                return MaskType
                        .valueOf(Settings.IMP.REGION_RESTRICTIONS_OPTIONS.MODE.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return MEMBER;
            }
        }
    }

}
