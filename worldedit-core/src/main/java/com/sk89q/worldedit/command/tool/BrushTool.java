/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command.tool;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.brush.MovableTool;
import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.mask.MaskedTargetBlock;
import com.boydti.fawe.object.pattern.PatternTraverser;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockType;
import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Builds a shape at the place being looked at.
 */
public class BrushTool implements TraceTool, MovableTool, ResettableTool, Serializable {

    protected static int MAX_RANGE = 500;
    protected int range = -1;
    private VisualMode visualMode = VisualMode.NONE;
    private TargetMode targetMode = TargetMode.TARGET_BLOCK_RANGE;
    private Mask mask = null;
    private Mask traceMask = null;
    private Mask sourceMask = null;
    private ResettableExtent transform;
    private Brush brush = new SphereBrush();
    @Nullable
    private Pattern material;
    private double size = 1;
    public String permission;

    private int targetOffset;

    private transient VisualExtent visualExtent;

    /**
     * Construct the tool.
     *
     * @param permission the permission to check before use is allowed
     */
    public BrushTool(String permission) {
        checkNotNull(permission);
        this.permission = permission;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission(permission);
    }

    public ResettableExtent getTransform() {
        return transform;
    }

    public void setTransform(ResettableExtent transform) {
        this.transform = transform;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getSourceMask() {
        return sourceMask;
    }

    @Override
    public boolean reset() {
        Brush br = getBrush();
        if (br instanceof ResettableTool) {
            return ((ResettableTool) br).reset();
        }
        return false;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setMask(Mask filter) {
        this.mask = filter;
    }

    /**
     * Get the mask used for identifying where to stop traces.
     *
     * @return the mask used to stop block traces
     */
    public @Nullable Mask getTraceMask() {
        return this.traceMask;
    }

    /**
     * Set the block mask used for identifying where to stop traces.
     *
     * @param traceMask the mask used to stop block traces
     */
    public void setTraceMask(@Nullable Mask traceMask) {
        this.traceMask = traceMask;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setSourceMask(Mask mask) {
        this.sourceMask = mask;
    }

    /**
     * Set the brush.
     *
     * @param brush tbe brush
     * @param permission the permission
     */
    public void setBrush(Brush brush, String permission) {
        this.brush = brush;
        this.permission = permission;
    }

    /**
     * Get the current brush.
     *
     * @return the current brush
     */
    public Brush getBrush() {
        return brush;
    }

    /**
     * Set the material.
     *
     * @param material the material
     */
    public void setFill(@Nullable Pattern material) {
        this.material = material;
    }

    /**
     * Get the material.
     *
     * @return the material
     */
    @Nullable public Pattern getMaterial() {
        return material;
    }

    /**
     * Get the set brush size.
     *
     * @return a radius
     */
    public double getSize() {
        return size;
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(double radius) {
        this.size = radius;
    }

    /**
     * Get the set brush range.
     *
     * @return the range of the brush in blocks
     */
    public int getRange() {
        return (range < 0) ? MAX_RANGE : Math.min(range, MAX_RANGE);
    }

    /**
     * Set the set brush range.
     *
     * @param range the range of the brush in blocks
     */
    public void setRange(int range) {
        this.range = range;
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {

        Location target = player.getBlockTrace(getRange(), true, traceMask);

        if (target == null) {
            player.printError("No block in sight!");
            return true;
        }

        BlockBag bag = session.getBlockBag(player);

        try (EditSession editSession = session.createEditSession(player)) {
            Request.request().setEditSession(editSession);
            if (mask != null) {
                Mask existingMask = editSession.getMask();

                if (existingMask == null) {
                    editSession.setMask(mask);
                } else if (existingMask instanceof MaskIntersection) {
                    ((MaskIntersection) existingMask).add(mask);
                } else {
                    MaskIntersection newMask = new MaskIntersection(existingMask);
                    newMask.add(mask);
                    editSession.setMask(newMask);
                }
            }

            Mask sourceMask1 = getSourceMask();
            if (sourceMask1 != null) {
                editSession.addSourceMask(sourceMask1);
            }
            ResettableExtent transform1 = getTransform();
            if (transform1 != null) {
                editSession.addTransform(transform1);
            }
            try {
                new PatternTraverser(this).reset(editSession);
                WorldEdit.getInstance().checkMaxBrushRadius(size);
                brush.build(editSession, target.toVector().toBlockPoint(), material, size);
            } catch (MaxChangedBlocksException e) {
                player.printError("Max blocks change limit reached.");
            } finally {
                session.remember(editSession);
            }
        } finally {
            if (bag != null) {
                bag.flushChanges();
            }
            Request.reset();
        }

        return true;
    }

    public BlockVector3 getPosition(EditSession editSession, Player player) {
        Location loc = player.getLocation();
        switch (targetMode) {
            case TARGET_BLOCK_RANGE:
            case TARGET_FACE_RANGE:
                return offset(trace(editSession, player, getRange(), true), loc).toBlockPoint();
            case FORWARD_POINT_PITCH: {
                int d = 0;
                float pitch = loc.getPitch();
                pitch = 23 - (pitch / 4);
                d += (int) (Math.sin(Math.toRadians(pitch)) * 50);
                final Vector3 vector = loc.getDirection().withY(0).normalize().multiply(d).add(loc.getX(), loc.getY(), loc.getZ());
                return offset(vector, loc).toBlockPoint();
            }
            case TARGET_POINT_HEIGHT: {
                final int height = loc.getBlockY();
                final int x = loc.getBlockX();
                final int z = loc.getBlockZ();
                int y;
                for (y = height; y > 0; y--) {
                    BlockType block = editSession.getBlockType(x, y, z);
                    if (block.getMaterial().isMovementBlocker()) {
                        break;
                    }
                }
                final int distance = (height - y) + 8;
                return offset(trace(editSession, player, distance, true), loc).toBlockPoint();
            }
            default:
                return null;
        }
    }

    private Vector3 offset(Vector3 target, Vector3 playerPos) {
        if (targetOffset == 0) return target;
        return target.subtract(target.subtract(playerPos).normalize().multiply(targetOffset));
    }

    private Vector3 trace(EditSession editSession, Player player, int range, boolean useLastBlock) {
        Mask mask = traceMask == null ? new SolidBlockMask(editSession) : traceMask;
        new MaskTraverser(mask).reset(editSession);
        MaskedTargetBlock tb = new MaskedTargetBlock(mask, player, range, 0.2);
        return TaskManager.IMP.sync(new RunnableVal<Vector3>() {
            @Override
            public void run(Vector3 value) {
                this.value = tb.getMaskedTargetBlock(useLastBlock);
            }
        });
    }


    public void setTargetOffset(int targetOffset) {
        this.targetOffset = targetOffset;
    }

    public void setTargetMode(TargetMode targetMode) {
        this.targetMode = targetMode != null ? targetMode : TargetMode.TARGET_BLOCK_RANGE;
    }

    public void setVisualMode(Player player, VisualMode visualMode) {
        if (visualMode == null) visualMode = VisualMode.NONE;
        if (this.visualMode != visualMode) {
            if (this.visualMode != VisualMode.NONE) {
                clear(player);
            }
            this.visualMode = visualMode;
            if (visualMode != VisualMode.NONE) {
                try {
                    queueVisualization(player);
                } catch (Throwable e) {
                    WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                }
            }
        }
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public int getTargetOffset() {
        return targetOffset;
    }

    public VisualMode getVisualMode() {
        return visualMode;
    }

    public void queueVisualization(Player player) {
        Fawe.get().getVisualQueue().queue(player);
    }

    @Deprecated
    public synchronized void visualize(Player player) throws WorldEditException {
        VisualMode mode = getVisualMode();
        if (mode == VisualMode.NONE) {
            return;
        }
        Brush brush = getBrush();
        if (brush == null) return;
        EditSessionBuilder builder = new EditSessionBuilder(player.getWorld())
                .player(player)
                .allowedRegionsEverywhere()
                .autoQueue(false)
                .blockBag(null)
                .changeSetNull()
                .combineStages(false);
        EditSession editSession = builder.build();
        VisualExtent newVisualExtent = new VisualExtent(editSession, builder.getQueue());
        BlockVector3 position = getPosition(editSession, player);
        if (position != null) {
            editSession.setExtent(newVisualExtent);
            switch (mode) {
                case POINT:
                    editSession.setBlock(position, VisualChunk.VISUALIZE_BLOCK);
                    break;
                case OUTLINE: {
                    new PatternTraverser(this).reset(editSession);
                    brush.build(editSession, position, getMaterial(), size);
                    break;
                }
            }
        }
        if (visualExtent != null) {
            // clear old data
            visualExtent.clear(newVisualExtent, player);
        }
        visualExtent = newVisualExtent;
        newVisualExtent.commit();
    }

    public void clear(Player player) {
        Fawe.get().getVisualQueue().dequeue(player);
        if (visualExtent != null) {
            visualExtent.clear(null, player);
        }
    }

    @Override
    public boolean move(Player player) {
        if (visualMode != VisualMode.NONE) {
            queueVisualization(player);
            return true;
        }
        return false;
    }
}
