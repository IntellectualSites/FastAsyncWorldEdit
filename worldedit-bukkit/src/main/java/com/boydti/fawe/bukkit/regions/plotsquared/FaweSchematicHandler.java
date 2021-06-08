package com.boydti.fawe.bukkit.regions.plotsquared;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.IOUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.generator.ClassicPlotWorld;
import com.plotsquared.core.inject.factory.ProgressSubscriberFactory;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.schematic.Schematic;
import com.plotsquared.core.queue.QueueCoordinator;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.MainUtil;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.core.util.task.RunnableVal;
import com.plotsquared.core.util.task.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompressedCompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.jnbt.fawe.CompressedSchematicTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.FastSchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.FastSchematicWriter;
import com.sk89q.worldedit.extent.clipboard.io.MCEditSchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicReader;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.jpountz.lz4.LZ4BlockInputStream;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

public class FaweSchematicHandler extends SchematicHandler {

    private static final AtomicBoolean exportingAll = new AtomicBoolean();

    public FaweSchematicHandler(@NotNull WorldUtil worldUtil, @NotNull ProgressSubscriberFactory subscriberFactory) {
        super(worldUtil, subscriberFactory);
    }
    private static final Logger logger = LogManagerCompat.getLogger();

    @Override
    public boolean restoreTile(QueueCoordinator queue, CompoundTag tag, int x, int y, int z) {
        return false;
    }

    @Override
    public void paste(final Schematic schematic,
                      final Plot plot,
                      final int xOffset,
                      final int yOffset,
                      final int zOffset,
                      final boolean autoHeight,
                      final PlotPlayer<?> actor,
                      final RunnableVal<Boolean> whenDone) {
        Runnable r = () -> {
            if (whenDone != null) {
                whenDone.value = false;
            }
            if (schematic == null) {
                TaskManager.runTask(whenDone);
                return;
            }
            BlockVector3 dimension = schematic.getClipboard().getDimensions();
            final int WIDTH = dimension.getX();
            final int LENGTH = dimension.getZ();
            final int HEIGHT = dimension.getY();
            // Validate dimensions
            CuboidRegion region = plot.getLargestRegion();
            if (((region.getMaximumPoint().getX() - region.getMinimumPoint().getX() + xOffset + 1) < WIDTH) || (
                (region.getMaximumPoint().getZ() - region.getMinimumPoint().getZ() + zOffset + 1) < LENGTH) || (HEIGHT
                > 256)) {
                TaskManager.runTask(whenDone);
                return;
            }
            // Calculate the optimal height to paste the schematic at
            final int y_offset_actual;
            if (autoHeight) {
                if (HEIGHT >= 256) {
                    y_offset_actual = yOffset;
                } else {
                    PlotArea pw = plot.getArea();
                    if (pw instanceof ClassicPlotWorld) {
                        y_offset_actual = yOffset + ((ClassicPlotWorld) pw).PLOT_HEIGHT;
                    } else {
                        y_offset_actual = yOffset + 1 + PlotSquared.platform().worldUtil()
                            .getHighestBlockSynchronous(plot.getWorldName(), region.getMinimumPoint().getX() + 1,
                                region.getMinimumPoint().getZ() + 1);
                    }
                }
            } else {
                y_offset_actual = yOffset;
            }

            final BlockVector3 to = BlockVector3
                .at(region.getMinimumPoint().getX() + xOffset, y_offset_actual, region.getMinimumPoint().getZ() + zOffset);

            try (EditSession editSession = new EditSessionBuilder(FaweAPI.getWorld(plot.getWorldName())).checkMemory(false)
                .fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build()) {
                final Clipboard clipboard = schematic.getClipboard();
                clipboard.paste(editSession, to, true, false, true);
                if (whenDone != null) {
                    whenDone.value = true;
                    TaskManager.runTask(whenDone);
                }
            }
        };
        if (Fawe.isMainThread()) {
            com.boydti.fawe.util.TaskManager.IMP.async(r);
        } else {
            r.run();
        }
    }

    @Override
    public boolean save(CompoundTag tag, String path) {
        if (tag == null) {
            logger.warn("Cannot save empty tag");
            return false;
        }
        try {
            File tmp = FileUtils.getFile(PlotSquared.platform().getDirectory(), path);
            tmp.getParentFile().mkdirs();
            if (tag instanceof CompressedCompoundTag) {
                CompressedCompoundTag cTag = (CompressedCompoundTag) tag;
                if (cTag instanceof CompressedSchematicTag) {
                    Clipboard clipboard = (Clipboard) cTag.getSource();
                    try (OutputStream stream = new FileOutputStream(tmp);
                        NBTOutputStream output = new NBTOutputStream(
                            new BufferedOutputStream(new PGZIPOutputStream(stream)))) {
                        new FastSchematicWriter(output).write(clipboard);
                    }
                } else {
                    try (OutputStream stream = new FileOutputStream(tmp);
                        BufferedOutputStream output = new BufferedOutputStream(new PGZIPOutputStream(stream))) {
                        LZ4BlockInputStream is = cTag.adapt(cTag.getSource());
                        IOUtil.copy(is, output);
                    }
                }
            } else {
                try (OutputStream stream = new FileOutputStream(tmp);
                    NBTOutputStream output = new NBTOutputStream(new PGZIPOutputStream(stream))) {
                    Map<String, Tag> map = tag.getValue();
                    output.writeNamedTag("Schematic", map.getOrDefault("Schematic", tag));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void upload(final CompoundTag tag, final UUID uuid, final String file, final RunnableVal<URL> whenDone) {
        if (tag == null) {
            logger.warn("Cannot save empty tag");
            com.plotsquared.core.util.task.TaskManager.runTask(whenDone);
            return;
        }
        final CompoundTag weTag = (CompoundTag) FaweCache.IMP.asTag(tag);
        upload(uuid, file, "schem", new RunnableVal<>() {
            @Override
            public void run(OutputStream output) {
                if (weTag instanceof CompressedSchematicTag) {
                    Clipboard clipboard = ((CompressedSchematicTag) weTag).getSource();
                    BuiltInClipboardFormat.SPONGE_SCHEMATIC.write(output, clipboard);
                }
                try {
                    try (PGZIPOutputStream gzip = new PGZIPOutputStream(output)) {
                        try (NBTOutputStream nos = new NBTOutputStream(gzip)) {
                            Map<String, Tag> map = weTag.getValue();
                            nos.writeNamedTag("Schematic", map.getOrDefault("Schematic", weTag));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, whenDone);
    }

    @Override
    public Schematic getSchematic(@NotNull InputStream is) {
        try {
            FastSchematicReader schematicReader = new FastSchematicReader(
                new NBTInputStream(new BufferedInputStream(new GZIPInputStream(new BufferedInputStream(is)))));
            Clipboard clip = schematicReader.read();
            return new Schematic(clip);
        } catch (IOException e) {
            if (e instanceof EOFException) {
                e.printStackTrace();
                return null;
            }
            try {
                SpongeSchematicReader schematicReader =
                    new SpongeSchematicReader(new NBTInputStream(new GZIPInputStream(is)));
                Clipboard clip = schematicReader.read();
                return new Schematic(clip);
            } catch (IOException e2) {
                if (e2 instanceof EOFException) {
                    e.printStackTrace();
                    return null;
                }
                try {
                    MCEditSchematicReader schematicReader =
                        new MCEditSchematicReader(new NBTInputStream(new GZIPInputStream(is)));
                    Clipboard clip = schematicReader.read();
                    return new Schematic(clip);
                } catch (IOException e3) {
                    e.printStackTrace();
                    logger.debug(
                        is + " | " + is.getClass().getCanonicalName() + " is not in GZIP format : " + e
                            .getMessage());
                }
            }
        }
        return null;
    }
}
