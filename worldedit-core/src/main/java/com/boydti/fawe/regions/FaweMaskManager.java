package com.boydti.fawe.regions;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.general.RegionFilter;

public abstract class FaweMaskManager<T> {

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

    @Deprecated
    public FaweMask getMask(final FawePlayer<T> player) {
        return getMask(player, MaskType.getDefaultMaskType());
    }

    public FaweMask getMask(final FawePlayer<T> player, MaskType type) {
        return getMask(player);
    }

    public boolean isValid(FaweMask mask) {
        return true;
    }

    public RegionFilter getFilter(String world) {
        return null;
    }

    private boolean hasMemberPermission(FawePlayer fp) {
        return fp.hasPermission("fawe." + getKey() + ".member");
    }

    public boolean isExclusive() {
        return false;
    }
}