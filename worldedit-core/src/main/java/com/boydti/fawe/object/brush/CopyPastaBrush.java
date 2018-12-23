package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.clipboard.ResizableClipboardBuilder;
import com.boydti.fawe.object.function.NullRegionFunction;
import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class CopyPastaBrush implements Brush, ResettableTool {

    private final LocalSession session;
    private final Player player;
    public boolean autoRotate, randomRotate;

    public CopyPastaBrush(Player player, LocalSession session, boolean randomRotate, boolean autoRotate) {
        session.setClipboard(null);
        this.player = player;
        this.session = session;
        this.randomRotate = randomRotate;
        this.autoRotate = autoRotate;
    }

    @Override
    public boolean reset() {
        session.setClipboard(null);
        BBC.BRUSH_RESET.send(player);
        return true;
    }

    @Override
    public void build(final EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        FawePlayer fp = editSession.getPlayer();
        ClipboardHolder clipboard = session.getExistingClipboard();
        if (clipboard == null) {
            if (editSession.getExtent() instanceof VisualExtent) {
                return;
            }
            Mask mask = editSession.getMask();
            if (mask == null) {
                mask = Masks.alwaysTrue();
            }
            final ResizableClipboardBuilder builder = new ResizableClipboardBuilder(editSession.getWorld());
            final int size2 = (int) (size * size);
            final int minY = position.getBlockY();
            mask = new AbstractDelegateMask(mask) {
                @Override
                public boolean test(BlockVector3 vector) {
                    if (super.test(vector) && vector.getBlockY() >= minY) {
                        BlockStateHolder block = editSession.getLazyBlock(vector);
                        if (!block.getBlockType().getMaterial().isAir()) {
                            builder.add(vector, EditSession.nullBlock, block);
                            return true;
                        }
                    }
                    return false;
                }
            };
            // Add origin
            mask.test(position);
            RecursiveVisitor visitor = new RecursiveVisitor(mask, new NullRegionFunction(), (int) size, editSession);
            visitor.visit(position);
            Operations.completeBlindly(visitor);
            // Build the clipboard
            Clipboard newClipboard = builder.build();
            newClipboard.setOrigin(position);
            ClipboardHolder holder = new ClipboardHolder(newClipboard);
            session.setClipboard(holder);
            int blocks = builder.size();
            BBC.COMMAND_COPY.send(fp, blocks);
            return;
        } else {
            AffineTransform transform = null;
            if (randomRotate) {
                if (transform == null) transform = new AffineTransform();
                int rotate = 90 * PseudoRandom.random.nextInt(4);
                transform = transform.rotateY(rotate);
            }
            if (autoRotate) {
                if (transform == null) transform = new AffineTransform();
                Location loc = editSession.getPlayer().getPlayer().getLocation();
                float yaw = loc.getYaw();
                float pitch = loc.getPitch();
                transform = transform.rotateY((-yaw) % 360);
                transform = transform.rotateX(pitch - 90);
            }
            if (transform != null && !transform.isIdentity()) {
                clipboard.setTransform(transform);
            }
            Clipboard faweClip = clipboard.getClipboard();
            Region region = faweClip.getRegion();

            Operation operation = clipboard
                    .createPaste(editSession)
                    .to(position.add(0, 1, 0))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.completeLegacy(operation);
            editSession.flushQueue();
        }
    }
}