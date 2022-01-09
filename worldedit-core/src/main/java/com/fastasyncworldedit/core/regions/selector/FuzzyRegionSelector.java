package com.fastasyncworldedit.core.regions.selector;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.fastasyncworldedit.core.regions.FuzzyRegion;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FuzzyRegionSelector extends PassthroughExtent implements RegionSelector {

    private final Actor actor;
    private FuzzyRegion region;
    private final ArrayList<BlockVector3> positions;

    public FuzzyRegionSelector(Actor actor, @Nullable World world, Mask mask) {
        super(WorldEdit.getInstance().newEditSessionBuilder().world(world)
                .actor(actor)
                .changeSetNull()
                .checkMemory(false)
                .build());
        this.actor = actor;
        this.region = new FuzzyRegion(world, getExtent(), mask);
        this.positions = new ArrayList<>();
        new MaskTraverser(mask).reset(getExtent());
    }

    @Nullable
    @Override
    public World getWorld() {
        return this.region.getWorld();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setWorld(@Nullable World world) {
        EditSession extent = WorldEdit.getInstance().newEditSessionBuilder().world(world)
                .actor(actor)
                .changeSetNull()
                .checkMemory(false)
                .build();
        new ExtentTraverser(this).setNext(extent);
        this.region.setWorld(world);
        this.region.setExtent(extent);
        new MaskTraverser(getMask()).reset(extent);
    }

    public Mask getMask() {
        return region.getMask();
    }

    @Override
    public boolean selectPrimary(BlockVector3 position, SelectorLimits limits) {
        setWorld(getWorld());
        new MaskTraverser(getMask()).reset(getExtent());
        positions.clear();
        positions.add(position);
        this.region = new FuzzyRegion(getWorld(), getExtent(), getMask());
        this.region.select(position);
        return true;
    }

    @Override
    public boolean selectSecondary(BlockVector3 position, SelectorLimits limits) {
        this.positions.add(position);
        new MaskTraverser(getMask()).reset(getExtent());
        this.region.select(position);
        return true;
    }

    @Override
    public void explainPrimarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        actor.print(Caption.of("fawe.worldedit.selector.selector.fuzzy.pos1", position, "(" + region.getVolume() + ")"));
    }

    @Override
    public void explainSecondarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        actor.print(Caption.of("fawe.worldedit.selector.selector.fuzzy.pos2", position, "(" + region.getVolume() + ")"));
    }

    @Override
    public void explainRegionAdjust(Actor actor, LocalSession session) {

    }

    @Override
    public BlockVector3 getPrimaryPosition() throws IncompleteRegionException {
        if (positions.isEmpty()) {
            throw new IncompleteRegionException();
        }
        return positions.get(0);
    }

    @Override
    public Region getRegion() throws IncompleteRegionException {
        return region;
    }

    @Override
    public Region getIncompleteRegion() {
        return region;
    }

    @Override
    public boolean isDefined() {
        return true;
    }

    @Override
    public int getArea() {
        return region.getArea();
    }

    @Override
    public void learnChanges() {

    }

    @Override
    public void clear() {
        positions.clear();
        this.region = new FuzzyRegion(getWorld(), getExtent(), getMask());
    }

    @Override
    public String getTypeName() {
        return "fuzzy";
    }

    @Override
    public List<String> getInformationLines() {
        return IntStream.range(0, positions.size())
                .mapToObj(i -> "Position " + i + ": " + positions.get(i)).collect(Collectors.toList());
    }

    @Override
    public List<BlockVector3> getVertices() {
        return positions;
    }

}
