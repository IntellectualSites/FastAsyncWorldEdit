package com.boydti.fawe.beta;

import com.sk89q.worldedit.regions.Region;

public class RegionFilter extends DelegateFilter {
    private final Region region;

    public RegionFilter(Filter parent, Region region) {
        super(parent);
        this.region = region;
    }

    @Override
    public Filter appliesChunk(int cx, int cz) {
        return getParent().appliesChunk(cx, cz);
    }

    @Override
    public Filter appliesLayer(IChunk chunk, int layer) {
        return getParent().appliesLayer(chunk, layer);
    }

    @Override
    public void applyBlock(FilterBlock block) {
        getParent().applyBlock(block);
    }

    @Override
    public void finishChunk(IChunk chunk) {
        getParent().finishChunk(chunk);
    }

    @Override
    public Filter newInstance(Filter other) {
        return new RegionFilter(other, region);
    }
}
