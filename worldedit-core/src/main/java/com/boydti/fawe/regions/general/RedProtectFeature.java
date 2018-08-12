package com.boydti.fawe.regions.general;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;

public class RedProtectFeature extends FaweMaskManager {
    public RedProtectFeature(String plugin) {
        super(plugin);
    }

    @Override
    public FaweMask getMask(FawePlayer player) {
        return null;
    }
}
