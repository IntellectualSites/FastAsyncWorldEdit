package com.boydti.fawe.object.schematic;

import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class Schematic {
    private final Clipboard clipboard;

    public Schematic(Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
    }

    /**
     * Get the schematic for a region
     *
     * @param region
     */
    public Schematic(Region region) {
        checkNotNull(region);
        checkNotNull(region.getWorld(), "World cannot be null (use the other constructor for the region)");
        EditSession session = new EditSessionBuilder(region.getWorld()).allowedRegionsEverywhere().autoQueue(false).build();
        this.clipboard = new BlockArrayClipboard(region, ReadOnlyClipboard.of(session, region));
    }

    public
    @Nullable
    Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Forwards to paste(world, to, true, true, null)
     *
     * @param world
     * @param to
     * @return
     */
    public EditSession paste(World world, Vector to) {
        return paste(world, to, true, true, null);
    }

    public void save(File file, ClipboardFormat format) throws IOException {
        checkNotNull(file);
        checkNotNull(format);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
        save(new FileOutputStream(file), format);
    }

    /**
     * Save this schematic to a stream
     *
     * @param stream
     * @param format
     * @throws IOException
     */
    public void save(OutputStream stream, ClipboardFormat format) throws IOException {
        checkNotNull(stream);
        checkNotNull(format);
        try (ClipboardWriter writer = format.getWriter(stream)) {
            writer.write(clipboard);
        }
    }

    public EditSession paste(World world, Vector to, boolean allowUndo, boolean pasteAir, @Nullable Transform transform) {
        return paste(world, to, allowUndo, pasteAir, true, transform);
    }

    /**
     * Paste this schematic in a world
     *
     * @param world
     * @param to
     * @param allowUndo
     * @param pasteAir
     * @param transform
     * @return
     */
    public EditSession paste(World world, Vector to, boolean allowUndo, boolean pasteAir, boolean copyEntities, @Nullable Transform transform) {
        checkNotNull(world);
        checkNotNull(to);
        Region region = clipboard.getRegion();
        EditSession editSession;
        if (world instanceof EditSession) {
            editSession = (EditSession) world;
        } else {
            EditSessionBuilder builder = new EditSessionBuilder(world).autoQueue(true).checkMemory(false).allowedRegionsEverywhere().limitUnlimited();
            if (allowUndo) {
                editSession = builder.build();
            } else {
                editSession = builder.changeSetNull().fastmode(true).build();
            }
        }
        Extent extent = clipboard;
        Mask sourceMask = editSession.getSourceMask();
        if (transform != null && !transform.isIdentity()) {
            extent = new BlockTransformExtent(clipboard, transform);
        } else if (sourceMask == null) {
            paste(editSession, to, pasteAir);
            editSession.flushQueue();
            return editSession;
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), editSession, to);
        if (transform != null && !transform.isIdentity()) {
            copy.setTransform(transform);
        }
        copy.setCopyingEntities(copyEntities);
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(extent);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        try {
            Operations.completeLegacy(copy);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
        editSession.flushQueue();
        return editSession;
    }

    public void paste(Extent extent, Vector to, boolean pasteAir, Transform transform) {
        checkNotNull(transform);
        Region region = clipboard.getRegion();
        Extent source = clipboard;
        if (transform != null) {
            source = new BlockTransformExtent(clipboard, transform);
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(source, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
        if (transform != null) {
            copy.setTransform(transform);
        }
        copy.setCopyBiomes(!(clipboard instanceof BlockArrayClipboard) || ((BlockArrayClipboard) clipboard).IMP.hasBiomes());
        if (extent instanceof EditSession) {
            EditSession editSession = (EditSession) extent;
            Mask sourceMask = editSession.getSourceMask();
            if (sourceMask != null) {
                new MaskTraverser(sourceMask).reset(extent);
                copy.setSourceMask(sourceMask);
                editSession.setSourceMask(null);
            }
        }
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        Operations.completeBlindly(copy);
    }

    public void paste(Extent extent, Vector to, final boolean pasteAir) {
        Region region = clipboard.getRegion().clone();
        final int maxY = extent.getMaximumPoint().getBlockY();
        final Vector bot = clipboard.getMinimumPoint();
        final Vector origin = clipboard.getOrigin();

        final boolean copyBiomes = !(clipboard instanceof BlockArrayClipboard) || ((BlockArrayClipboard) clipboard).IMP.hasBiomes();

        // Optimize for BlockArrayClipboard
        if (clipboard instanceof BlockArrayClipboard && region instanceof CuboidRegion) {
            // To is relative to the world origin (player loc + small clipboard offset) (As the positions supplied are relative to the clipboard min)
            final int relx = to.getBlockX() + bot.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() + bot.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() + bot.getBlockZ() - origin.getBlockZ();
            BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
            if (copyBiomes) {
                bac.IMP.forEach(new FaweClipboard.BlockReader() {
                    MutableBlockVector2D mpos2d = new MutableBlockVector2D();
                    {
                        mpos2d.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    }
                    @Override
                    public void run(int x, int y, int z, BlockState block) {
                        try {
                            int xx = x + relx;
                            int zz = z + relz;
                            if (xx != mpos2d.getBlockX() || zz != mpos2d.getBlockZ()) {
                                mpos2d.setComponents(xx, zz);
                                extent.setBiome(mpos2d, bac.IMP.getBiome(x, z));
                            }
                            if (!pasteAir && block.getBlockType().getMaterial().isAir()) {
                                return;
                            }
                            extent.setBlock(xx, y + rely, zz, block);
                        } catch (WorldEditException e) { throw new RuntimeException(e);}
                    }
                }, true);
            } else {
                bac.IMP.forEach(new FaweClipboard.BlockReader() {
                    @Override
                    public void run(int x, int y, int z, BlockState block) {
                        try {
                            extent.setBlock(x + relx, y + rely, z + relz, block);
                        } catch (WorldEditException e) { throw new RuntimeException(e);}
                    }
                }, pasteAir);
            }
        } else {
            // To must be relative to the clipboard origin ( player location - clipboard origin ) (as the locations supplied are relative to the world origin)
            final int relx = to.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() - origin.getBlockZ();
            RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                MutableBlockVector2D mpos2d_2 = new MutableBlockVector2D();
                MutableBlockVector2D mpos2d = new MutableBlockVector2D();
                {
                    mpos2d.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE);
                }
                @Override
                public boolean apply(Vector mutable) throws WorldEditException {
                    BlockStateHolder block = clipboard.getBlock(mutable);
                    int xx = mutable.getBlockX() + relx;
                    int zz = mutable.getBlockZ() + relz;
                    if (copyBiomes && xx != mpos2d.getBlockX() && zz != mpos2d.getBlockZ()) {
                        mpos2d.setComponents(xx, zz);
                        extent.setBiome(mpos2d, clipboard.getBiome(mpos2d_2.setComponents(mutable.getBlockX(), mutable.getBlockZ())));
                    }
                    if (!pasteAir && block.getBlockType().getMaterial().isAir()) {
                        return false;
                    }
                    extent.setBlock(xx, mutable.getBlockY() + rely, zz, block);
                    return false;
                }
            }, (HasFaweQueue) (null));
            Operations.completeBlindly(visitor);
        }
        // Entity offset is the paste location subtract the clipboard origin (entity's location is already relative to the world origin)
        final int entityOffsetX = to.getBlockX() - origin.getBlockX();
        final int entityOffsetY = to.getBlockY() - origin.getBlockY();
        final int entityOffsetZ = to.getBlockZ() - origin.getBlockZ();
        // entities
        for (Entity entity : clipboard.getEntities()) {
            Location pos = entity.getLocation();
            Location newPos = new Location(pos.getExtent(), pos.getX() + entityOffsetX, pos.getY() + entityOffsetY, pos.getZ() + entityOffsetZ, pos.getYaw(), pos.getPitch());
            extent.createEntity(newPos, entity.getState());
        }
    }
}
