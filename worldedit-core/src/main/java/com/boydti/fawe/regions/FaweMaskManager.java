package com.boydti.fawe.regions;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.regions.general.RegionFilter;
import com.sk89q.worldedit.entity.Player;

public abstract class FaweMaskManager {

    public enum MaskType {
        OWNER,
        MEMBER

        ;

        public static MaskType getDefaultMaskType() {
            try {
                return MaskType.valueOf(Settings.IMP.REGION_RESTRICTIONS_OPTIONS.MODE.toUpperCase());
            } catch (Exception ignore) {
                return MEMBER;
            }
        }
    }

    private final String key;

    public FaweMaskManager(final String plugin) {
        this.key = plugin.toLowerCase();
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }

    public abstract FaweMask getMask(final Player player, MaskType type);

    public boolean isValid(FaweMask mask) {
        return true;
    }

    public RegionFilter getFilter(String world) {
        return null;
    }

    private boolean hasMemberPermission(Player player) {
        return player.hasPermission("fawe." + getKey() + ".member");
    }

    public boolean isExclusive() {
        return false;
    }
}
