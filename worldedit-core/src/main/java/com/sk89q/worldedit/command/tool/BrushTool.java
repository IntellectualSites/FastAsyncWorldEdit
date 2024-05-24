/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command.tool;

import com.fastasyncworldedit.core.command.tool.MovableTool;
import com.fastasyncworldedit.core.command.tool.ResettableTool;
import com.fastasyncworldedit.core.command.tool.TargetMode;
import com.fastasyncworldedit.core.command.tool.brush.BrushSettings;
import com.fastasyncworldedit.core.command.tool.scroll.Scroll;
import com.fastasyncworldedit.core.command.tool.scroll.ScrollTool;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.fastasyncworldedit.core.function.mask.MaskedTargetBlock;
import com.fastasyncworldedit.core.function.pattern.PatternTraverser;
import com.fastasyncworldedit.core.util.BrushCache;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds a shape at the place being looked at.
 */
public class BrushTool
        //FAWE start - All implements but TraceTool
        implements DoubleActionTraceTool, ScrollTool, MovableTool, ResettableTool, Serializable, TraceTool {
    //    TODO:
    // Serialize methods
    // serialize BrushSettings (primary and secondary only if different)
    // set transient values e.g., context


    enum BrushAction {
        PRIMARY,
        SECONDARY
    }
    //FAWE end

    protected static int MAX_RANGE = 500;
    protected int range = -1;
    private Mask traceMask = null;
    //FAWE start
    protected static int DEFAULT_RANGE = 240; // 500 is laggy as the default
    private TargetMode targetMode = TargetMode.TARGET_BLOCK_RANGE;
    private int targetOffset;

    private transient BrushSettings primary = new BrushSettings();
    private transient BrushSettings secondary = new BrushSettings();
    private transient BrushSettings context = primary;

    private transient BaseItem holder;
    //FAWE end

    /**
     * Construct the tool.
     *
     * @param permission the permission to check before use is allowed
     */
    public BrushTool(String permission) {
        checkNotNull(permission);
        getContext().addPermission(permission);
    }

    //FAWE start
    public BrushTool() {
    }

    public void setHolder(BaseItem holder) {
        this.holder = holder;
    }

    public boolean isSet() {
        return primary.getBrush() != null || secondary.getBrush() != null;
    }

    public void update() {
        if (holder != null) {
            BrushCache.setTool(holder, this);
        }
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeBoolean(primary == secondary);
        stream.writeObject(primary);
        if (primary != secondary) {
            stream.writeObject(secondary);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        boolean multi = stream.readBoolean();
        primary = (BrushSettings) stream.readObject();
        if (multi) {
            secondary = (BrushSettings) stream.readObject();
        } else {
            secondary = primary;
        }
        context = primary;
    }

    public BrushSettings getContext() {
        BrushSettings tmp = context;
        if (tmp == null) {
            context = tmp = primary;
        }
        return tmp;
    }

    public void setContext(BrushSettings context) {
        this.context = context;
    }

    @Override
    public boolean canUse(Actor player) {
        if (primary == secondary) {
            return primary.canUse(player);
        }
        return primary.canUse(player) && secondary.canUse(player);
    }

    public ResettableExtent getTransform() {
        return getContext().getTransform();
    }

    public BrushSettings getPrimary() {
        return primary;
    }

    public BrushSettings getSecondary() {
        return secondary;
    }

    public BrushSettings getOffHand() {
        return context == primary ? secondary : primary;
    }

    public void setPrimary(BrushSettings primary) {
        checkNotNull(primary);
        this.primary = primary;
        this.context = primary;
        update();
    }

    public void setSecondary(BrushSettings secondary) {
        checkNotNull(secondary);
        this.secondary = secondary;
        this.context = secondary;
        update();
    }

    public void setTransform(ResettableExtent transform) {
        getContext().setTransform(transform);
        update();
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    //TODO A better description is needed here to explain what makes a source-mask different from a regular mask.
    public Mask getSourceMask() {
        return getContext().getSourceMask();
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
        this.getContext().setMask(filter);
        update();
    }
    //FAWE end

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getMask() {
        return getContext().getMask();
    }

    /**
     * Get the mask used for identifying where to stop traces.
     *
     * @return the mask used to stop block traces
     */
    @Nullable
    public Mask getTraceMask() {
        return this.traceMask;
    }

    /**
     * Set the block mask used for identifying where to stop traces.
     *
     * @param traceMask the mask used to stop block traces
     */
    public void setTraceMask(@Nullable Mask traceMask) {
        this.traceMask = traceMask;
        update();
    }

    //FAWE start

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setSourceMask(Mask filter) {
        this.getContext().setSourceMask(filter);
        update();
    }
    //FAWE end

    /**
     * Set the brush.
     *
     * @param brush      the brush
     * @param permission the permission
     */
    public void setBrush(Brush brush, String permission) {
        //FAWE start - We use our own logic
        BrushSettings current = getContext();
        current.clearPerms();
        current.setBrush(brush);
        current.addPermission(permission);
        update();
        //FAWE end
    }

    /**
     * Get the current brush.
     *
     * @return the current brush
     */
    public Brush getBrush() {
        return getContext().getBrush();
    }

    /**
     * Set the material.
     *
     * @param material the material
     */
    public void setFill(@Nullable Pattern material) {
        this.getContext().setFill(material);
    }

    /**
     * Get the material.
     *
     * @return the material
     */
    @Nullable
    public Pattern getMaterial() {
        return getContext().getMaterial();
    }

    /**
     * Get the set brush size.
     *
     * @return a radius
     */
    public double getSize() {
        return getContext().getSize();
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(double radius) {
        this.getContext().setSize(radius);
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(Expression radius) {
        this.getContext().setSize(radius);
    }

    /**
     * Get the set brush range.
     *
     * @return the range of the brush in blocks
     */
    public int getRange() {
        return (range < 0) ? DEFAULT_RANGE : Math.min(range, MAX_RANGE);
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
    public boolean actPrimary(
            Platform server, LocalConfiguration config, Player player,
            LocalSession session
    ) {
        //FAWE start - Use logic previously declared as FAWE-like
        return act(BrushAction.PRIMARY, player, session);
        //FAWE end
    }

    //FAWE start
    public BlockVector3 getPosition(EditSession editSession, Player player) {
        Location loc = player.getLocation();
        switch (targetMode) {
            case TARGET_BLOCK_RANGE:
                return offset(trace(editSession, player, getRange(), true), loc).toBlockPoint();
            case FORWARD_POINT_PITCH: {
                int d = 0;
                float pitch = loc.getPitch();
                pitch = 23 - (pitch / 4);
                d += (int) (Math.sin(Math.toRadians(pitch)) * 50);
                final Vector3 vector = loc.getDirection().withY(0).normalize().multiply(d)
                        .add(loc.getX(), loc.getY(), loc.getZ());
                return offset(vector, loc).toBlockPoint();
            }
            case TARGET_POINT_HEIGHT: {
                final int height = loc.getBlockY();
                final int x = loc.getBlockX();
                final int z = loc.getBlockZ();
                int y;
                for (y = height; y > editSession.getMinY(); y--) {
                    BlockType block = editSession.getBlockType(x, y, z);
                    if (block.getMaterial().isMovementBlocker()) {
                        break;
                    }
                }
                final int distance = (height - y) + 8;
                return offset(trace(editSession, player, distance, true), loc).toBlockPoint();
            }
            case TARGET_FACE_RANGE:
                return offset(trace(editSession, player, getRange(), true), loc).toBlockPoint();
            default:
                return null;
        }
    }

    private Vector3 offset(Vector3 target, Vector3 playerPos) {
        if (targetOffset == 0) {
            return target;
        }
        return target.subtract(target.subtract(playerPos).normalize().multiply(targetOffset));
    }

    private Vector3 trace(EditSession editSession, Player player, int range, boolean useLastBlock) {
        Mask mask = traceMask == null ? new SolidBlockMask(editSession) : traceMask;
        new MaskTraverser(mask).reset(editSession);
        MaskedTargetBlock tb = new MaskedTargetBlock(mask, player, range, 0.2);
        return tb.getMaskedTargetBlock(useLastBlock);
    }

    public boolean act(BrushAction action, Player player, LocalSession session) {
        switch (action) {
            case PRIMARY:
                setContext(primary);
                break;
            case SECONDARY:
                setContext(secondary);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
        BrushSettings current = getContext();
        Brush brush = current.getBrush();
        if (brush == null) {
            return false;
        }

        if (!current.canUse(player)) {
            player.print(
                    Caption.of("fawe.error.no-perm", StringMan.join(current.getPermissions(), ",")));
            return false;
        }
        try (EditSession editSession = session.createEditSession(player, current.toString())) {
            Location target = player.getBlockTrace(getRange(), true, traceMask);

            if (target == null) {
                editSession.cancel();
                player.print(Caption.of("worldedit.tool.no-block"));
                return true;
            }
            BlockBag bag = session.getBlockBag(player);

            Request.request().setEditSession(editSession);
            Mask mask = current.getMask();
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

            Mask sourceMask = current.getSourceMask();
            if (sourceMask != null) {
                editSession.addSourceMask(sourceMask);
            }
            ResettableExtent transform = current.getTransform();
            if (transform != null) {
                editSession.addTransform(transform);
            }
            try {
                new PatternTraverser(current).reset(editSession);
                double size = current.getSize();
                WorldEdit.getInstance().checkMaxBrushRadius(size, player);
                brush.build(editSession, target.toBlockPoint(), current.getMaterial(), size);
            } catch (MaxChangedBlocksException e) {
                player.print(Caption.of("worldedit.tool.max-block-changes"));
            } finally {
                session.remember(editSession);
                if (bag != null) {
                    bag.flushChanges();
                }
            }
        } finally {
            Request.reset();
        }

        return true;
    }

    @Override
    public boolean actSecondary(
            Platform server, LocalConfiguration config, Player player,
            LocalSession session
    ) {
        return act(BrushAction.SECONDARY, player, session);
    }


    public void setScrollAction(Scroll scrollAction) {
        this.getContext().setScrollAction(scrollAction);
        update();
    }

    public void setTargetOffset(int targetOffset) {
        this.targetOffset = targetOffset;
        update();
    }

    public void setTargetMode(TargetMode targetMode) {
        this.targetMode = targetMode != null ? targetMode : TargetMode.TARGET_BLOCK_RANGE;
        update();
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public int getTargetOffset() {
        return targetOffset;
    }

    @Override
    public boolean increment(Player player, int amount) {
        BrushSettings current = getContext();
        Scroll tmp = current.getScrollAction();
        if (tmp != null) {
            tmp.setTool(this);
            return tmp.increment(player, amount);
        }
        return false;
    }

    @Override
    public boolean move(Player player) {
        return false;
    }
    //FAWE end
}
