package com.boydti.fawe.regions.general.integrations.plotquared;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.IOUtil;
import com.boydti.fawe.util.TaskManager;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.SchematicHandler;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompressedCompoundTag;
import com.sk89q.jnbt.CompressedSchematicTag;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jpountz.lz4.LZ4BlockInputStream;

public class FaweSchematicHandler extends SchematicHandler {
    @Override
    public boolean restoreTile(LocalBlockQueue queue, CompoundTag compoundTag, int x, int y, int z) {
        if (queue instanceof FaweLocalBlockQueue) {
            queue.setTile(x, y, z, compoundTag);
            return true;
        }
        return false;
    }

    @Override
    public void getCompoundTag(final String world, final Set<CuboidRegion> regions, final RunnableVal<CompoundTag> whenDone) {
        TaskManager.IMP.async(() -> {
            Location[] corners = MainUtil.getCorners(world, regions);
            Location pos1 = corners[0];
            Location pos2 = corners[1];
            final CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
            final EditSession editSession = new EditSessionBuilder(world).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();

            final int mx = pos1.getX();
            final int my = pos1.getY();
            final int mz = pos1.getZ();

            ReadOnlyClipboard clipboard = ReadOnlyClipboard.of(editSession, region);

            Clipboard holder = new BlockArrayClipboard(region, clipboard);
            CompressedSchematicTag tag = new CompressedSchematicTag(holder);
            whenDone.run(tag);
        });
    }

    @Override
    public boolean save(CompoundTag tag, String path) {
        if (tag == null) {
            PlotSquared.debug("&cCannot save empty tag");
            return false;
        }
        try {
            File tmp = MainUtil.getFile(PlotSquared.get().IMP.getDirectory(), path);
            tmp.getParentFile().mkdirs();
            if (tag instanceof CompressedCompoundTag) {
                CompressedCompoundTag cTag = (CompressedCompoundTag) tag;
                if (cTag instanceof CompressedSchematicTag) {
                    Clipboard clipboard = (Clipboard) cTag.getSource();
                    try (OutputStream stream = new FileOutputStream(tmp); NBTOutputStream output = new NBTOutputStream(new BufferedOutputStream(new PGZIPOutputStream(stream)))) {
                        new SpongeSchematicWriter(output).write(clipboard);
                    }
                } else {
                    try (OutputStream stream = new FileOutputStream(tmp); BufferedOutputStream output = new BufferedOutputStream(new PGZIPOutputStream(stream))) {
                        LZ4BlockInputStream is = cTag.adapt(cTag.getSource());
                        IOUtil.copy(is, stream);
                    }
                }
            } else {
                try (OutputStream stream = new FileOutputStream(tmp); NBTOutputStream output = new NBTOutputStream(new PGZIPOutputStream(stream))) {
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
            PlotSquared.debug("&cCannot save empty tag");
            com.github.intellectualsites.plotsquared.plot.util.TaskManager.runTask(whenDone);
            return;
        }
        MainUtil.upload(uuid, file, "schematic", new RunnableVal<OutputStream>() {
            @Override
            public void run(OutputStream output) {
                try {
                    try (PGZIPOutputStream gzip = new PGZIPOutputStream(output)) {
                        CompoundTag weTag = (CompoundTag) FaweCache.IMP.asTag(tag);
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
}
