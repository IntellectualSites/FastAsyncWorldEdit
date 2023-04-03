package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.FaweCache;
import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicReader;
import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicWriter;
import com.fastasyncworldedit.core.jnbt.CompressedCompoundTag;
import com.fastasyncworldedit.core.jnbt.CompressedSchematicTag;
import com.fastasyncworldedit.core.util.IOUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.generator.ClassicPlotWorld;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.schematic.Schematic;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.task.RunnableVal;
import com.plotsquared.core.util.task.TaskManager;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.MCEditSchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicReader;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.jpountz.lz4.LZ4BlockInputStream;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
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

@SuppressWarnings("unused")
public class FaweDelegateSchematicHandler {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private static final AtomicBoolean exportingAll = new AtomicBoolean();

    @Deprecated
    public void paste(
            final Schematic schematic,
            final Plot plot,
            final int xOffset,
            final int yOffset,
            final int zOffset,
            final boolean autoHeight,
            final RunnableVal<Boolean> whenDone
    ) {
        paste(schematic, plot , xOffset, yOffset, zOffset, autoHeight, null, whenDone);
    }

    public void paste(
            final Schematic schematic,
            final Plot plot,
            final int xOffset,
            final int yOffset,
            final int zOffset,
            final boolean autoHeight,
            final PlotPlayer<?> actor,
            final RunnableVal<Boolean> whenDone
    ) {
        Runnable r = () -> {
            if (whenDone != null) {
                whenDone.value = false;
            }
            if (schematic == null) {
                if (whenDone != null) {
                    TaskManager.runTask(whenDone);
                }
                return;
            }
            BlockVector3 dimension = schematic.getClipboard().getDimensions();
            final int WIDTH = dimension.getX();
            final int LENGTH = dimension.getZ();
            final int HEIGHT = dimension.getY();
            final int worldHeight = plot.getArea().getMaxGenHeight() - plot.getArea().getMinGenHeight() + 1;
            // Validate dimensions
            CuboidRegion region = plot.getLargestRegion();
            boolean sizeMismatch =
                    ((region.getMaximumPoint().getX() - region.getMinimumPoint().getX() + xOffset + 1) < WIDTH) || (
                            (region.getMaximumPoint().getZ() - region.getMinimumPoint().getZ() + zOffset + 1) < LENGTH) || (HEIGHT
                            > worldHeight);
            if (!Settings.Schematics.PASTE_MISMATCHES && sizeMismatch) {
                if (actor != null) {
                    actor.sendMessage(TranslatableCaption.of("schematics.schematic_size_mismatch"));
                }
                TaskManager.runTask(whenDone);
                return;
            }
            if (((region.getMaximumPoint().getX() - region.getMinimumPoint().getX() + xOffset + 1) < WIDTH) || (
                    (region.getMaximumPoint().getZ() - region.getMinimumPoint().getZ() + zOffset + 1) < LENGTH) || (HEIGHT
                    > worldHeight)) {
                if (whenDone != null) {
                    TaskManager.runTask(whenDone);
                }
                return;
            }

            try (EditSession editSession = WorldEdit
                    .getInstance()
                    .newEditSessionBuilder()
                    .world(FaweAPI.getWorld(plot.getWorldName()))
                    .checkMemory(false)
                    .fastMode(true)
                    .limitUnlimited()
                    .changeSetNull()
                    .build()) {
                // Calculate the optimal height to paste the schematic at
                final int y_offset_actual;
                if (autoHeight) {
                    if (HEIGHT >= worldHeight) {
                        y_offset_actual = yOffset;
                    } else {
                        PlotArea pw = plot.getArea();
                        if (pw instanceof ClassicPlotWorld) {
                            y_offset_actual = yOffset + pw.getMinBuildHeight() + ((ClassicPlotWorld) pw).PLOT_HEIGHT;
                        } else {
                            y_offset_actual = yOffset + pw.getMinBuildHeight() + editSession.getHighestTerrainBlock(region
                                    .getMinimumPoint()
                                    .getX() + 1, region.getMinimumPoint().getZ() + 1, pw.getMinGenHeight(), pw.getMaxGenHeight()
                            );
                        }
                    }
                } else {
                    y_offset_actual = yOffset;
                }

                final BlockVector3 to = BlockVector3
                        .at(region.getMinimumPoint().getX() + xOffset, y_offset_actual, region.getMinimumPoint().getZ() + zOffset);
                final Clipboard clipboard = schematic.getClipboard();
                clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());
                clipboard.paste(editSession, to, true, false, true);
                if (whenDone != null) {
                    whenDone.value = true;
                    TaskManager.runTask(whenDone);
                }
            }
        };
        if (Fawe.isTickThread()) {
            com.fastasyncworldedit.core.util.TaskManager.taskManager().async(r);
        } else {
            r.run();
        }
    }

    public boolean save(CompoundTag tag, String path) {
        if (tag == null) {
            LOGGER.warn("Cannot save empty tag");
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
                                 new BufferedOutputStream(new ParallelGZIPOutputStream(stream)))) {
                        new FastSchematicWriter(output).write(clipboard);
                    }
                } else {
                    try (OutputStream stream = new FileOutputStream(tmp);
                         BufferedOutputStream output = new BufferedOutputStream(new ParallelGZIPOutputStream(stream))) {
                        LZ4BlockInputStream is = cTag.adapt(cTag.getSource());
                        IOUtil.copy(is, output);
                    }
                }
            } else {
                try (OutputStream stream = new FileOutputStream(tmp);
                     NBTOutputStream output = new NBTOutputStream(new ParallelGZIPOutputStream(stream))) {
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

    public void upload(final CompoundTag tag, final UUID uuid, final String file, final RunnableVal<URL> whenDone) {
        if (tag == null) {
            LOGGER.warn("Cannot save empty tag");
            if (whenDone != null) {
                TaskManager.runTask(whenDone);
            }
            return;
        }
        final CompoundTag weTag = (CompoundTag) FaweCache.INSTANCE.asTag(tag);
        SchematicHandler.upload(uuid, file, "schem", new RunnableVal<>() {
            @Override
            public void run(OutputStream output) {
                if (weTag instanceof CompressedSchematicTag) {
                    Clipboard clipboard = ((CompressedSchematicTag) weTag).getSource();
                    BuiltInClipboardFormat.FAST.write(output, clipboard);
                }
                try {
                    try (ParallelGZIPOutputStream gzip = new ParallelGZIPOutputStream(output)) {
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

    public Schematic getSchematic(@Nonnull InputStream is) {
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
                    LOGGER.warn("{} | {} : {}", is, is.getClass().getCanonicalName(), e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}
