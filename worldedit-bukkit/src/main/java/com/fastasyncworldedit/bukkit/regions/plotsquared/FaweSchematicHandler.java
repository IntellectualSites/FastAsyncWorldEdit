package com.fastasyncworldedit.bukkit.regions.plotsquared;

import com.fastasyncworldedit.FaweCache;
import com.fastasyncworldedit.object.clipboard.ReadOnlyClipboard;
import com.fastasyncworldedit.object.io.PGZIPOutputStream;
import com.fastasyncworldedit.util.EditSessionBuilder;
import com.fastasyncworldedit.util.IOUtil;
import com.fastasyncworldedit.util.TaskManager;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.schematic.Schematic;
import com.plotsquared.core.queue.LocalBlockQueue;
import com.plotsquared.core.util.MainUtil;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.task.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompressedCompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.jnbt.fawe.CompressedSchematicTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.FastSchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.FastSchematicWriter;
import com.sk89q.worldedit.extent.clipboard.io.MCEditSchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.SpongeSchematicReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import net.jpountz.lz4.LZ4BlockInputStream;
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
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.bukkit.Bukkit.getWorld;

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
            World adaptedWorld = BukkitAdapter.adapt(getWorld(world));
            final CuboidRegion region = new CuboidRegion(BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ()), BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ()));
            final EditSession editSession = new EditSessionBuilder(adaptedWorld).checkMemory(false).fastmode(true).limitUnlimited().changeSetNull().autoQueue(false).build();

            ReadOnlyClipboard clipboard = ReadOnlyClipboard.of(editSession, region, false, true);

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
                        new FastSchematicWriter(output).write(clipboard);
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
            com.plotsquared.core.util.task.TaskManager.runTask(whenDone);
            return;
        }
        final CompoundTag weTag = (CompoundTag) FaweCache.IMP.asTag(tag);
        MainUtil.upload(uuid, file, "schem", new RunnableVal<OutputStream>() {
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
                    PlotSquared.debug(
                        is.toString() + " | " + is.getClass().getCanonicalName() + " is not in GZIP format : " + e
                            .getMessage());
                }
            }
        }
        return null;
    }
}
