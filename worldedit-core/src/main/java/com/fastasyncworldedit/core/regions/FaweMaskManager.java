package com.fastasyncworldedit.core.regions;

import com.fastasyncworldedit.core.configuration.Settings;
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
     * Get a {@link FaweMask} for the given player and {@link MaskType}. If isWhitelist is false, will return a "blacklist" mask.
     */
    public abstract FaweMask getMask(final Player player, MaskType type, boolean isWhitelist);

    /**
     * Get a {@link FaweMask} for the given player and {@link MaskType}. If isWhitelist is false, will return a "blacklist" mask.
     *
     * @since 2.7.0
     */
    public FaweMask getMask(final Player player, MaskType type, boolean isWhitelist, boolean notify) {
        return getMask(player, type, isWhitelist);
    }

    public boolean isExclusive() {
        return Settings.settings().REGION_RESTRICTIONS_OPTIONS.EXCLUSIVE_MANAGERS.contains(this.key);
    }

    public enum MaskType {
        OWNER,
        MEMBER;

        public static MaskType getDefaultMaskType() {
            try {
                return MaskType
                        .valueOf(Settings.settings().REGION_RESTRICTIONS_OPTIONS.MODE.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                return MEMBER;
            }
        }
    }

}
