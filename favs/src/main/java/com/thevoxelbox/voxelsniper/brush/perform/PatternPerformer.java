package com.thevoxelbox.voxelsniper.brush.perform;

import com.boydti.fawe.bukkit.wrapper.AsyncBlock;
import com.boydti.fawe.object.pattern.PatternTraverser;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.block.Block;

public class PatternPerformer extends vPerformer {
    private String info;
    private Pattern pattern;
    private Extent extent;
    private MutableBlockVector mutable = new MutableBlockVector();

    @Override
    public void info(Message vm) {
        vm.performerName(this.name + ": " + info);
        vm.voxel();
    }

    @Override
    public void init(SnipeData snipeData) {
        this.w = snipeData.getWorld();
        this.extent = snipeData.getExtent();
        this.info = snipeData.getPatternInfo();
        this.pattern = snipeData.getPattern();
        new PatternTraverser(pattern).reset(extent);
    }

    @Override
    public void perform(AsyncBlock block) {
        mutable.setComponents(block.getX(), block.getY(), block.getZ());
        try {
            pattern.apply(extent, mutable, mutable);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }
}
