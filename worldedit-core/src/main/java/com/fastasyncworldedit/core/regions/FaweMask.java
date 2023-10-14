package com.fastasyncworldedit.core.regions;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nonnull;
import java.util.Objects;

public class FaweMask implements IDelegateRegion {

    private final Region region;

    public FaweMask(@Nonnull Region region) {
        this.region = Objects.requireNonNull(region);
    }

    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * Test if the mask is still valid
     *
     * @param player player to test
     * @param type   type of mask
     * @return if still valid
     */
    public boolean isValid(Player player, FaweMaskManager.MaskType type) {
        return false;
    }

    /**
     * Test if the mask is still valid
     *
     * @param player player to test
     * @param type   type of mask
     * @param notify if the player should be notified
     * @return if still valid
     * @since 2.7.0
     */
    public boolean isValid(Player player, FaweMaskManager.MaskType type, boolean notify) {
        return isValid(player, type);
    }

    @Override
    public Region clone() {
        throw new UnsupportedOperationException("Clone not supported");
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.REMOVING_BLOCKS;
    }

}
