package com.boydti.fawe.object.regions.selector;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.regions.FuzzyRegion;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class FuzzyRegionSelector extends AbstractDelegateExtent implements RegionSelector {

    private final Player player;
    private FuzzyRegion region;
    private ArrayList<BlockVector3> positions;

    public FuzzyRegionSelector(Player player, @Nullable World world, Mask mask) {
        super(new EditSessionBuilder(world)
                .player(FawePlayer.wrap(player))
                .changeSetNull()
                .checkMemory(false)
                .autoQueue(true)
                .build());
        this.player = player;
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
    public void setWorld(@Nullable World world) {
        EditSession extent = new EditSessionBuilder(world)
                .player(FawePlayer.wrap(player))
                .changeSetNull()
                .checkMemory(false)
                .autoQueue(true)
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
        this.region.select(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        return true;
    }

    @Override
    public boolean selectSecondary(BlockVector3 position, SelectorLimits limits) {
        this.positions.add(position);
        new MaskTraverser(getMask()).reset(getExtent());
        this.region.select(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        return true;
    }

    @Override
    public void explainPrimarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        int size = this.region.getArea();
        BBC.SELECTOR_FUZZY_POS1.send(player, position, "(" + region.getArea() + ")");
    }

    @Override
    public void explainSecondarySelection(Actor actor, LocalSession session, BlockVector3 position) {
        int size = this.region.getArea();
        BBC.SELECTOR_FUZZY_POS2.send(player, position, "(" + region.getArea() + ")");
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
        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            lines.add("Position " + i + ": " + positions.get(i));
        }
        return lines;
    }

}
