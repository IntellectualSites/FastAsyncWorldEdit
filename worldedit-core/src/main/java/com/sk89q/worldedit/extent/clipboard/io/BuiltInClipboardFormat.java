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

package com.sk89q.worldedit.extent.clipboard.io;

import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicReaderV2;
import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicReaderV3;
import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicWriterV2;
import com.fastasyncworldedit.core.extent.clipboard.io.FastSchematicWriterV3;
import com.fastasyncworldedit.core.extent.clipboard.io.schematic.MinecraftStructure;
import com.fastasyncworldedit.core.extent.clipboard.io.schematic.PNGWriter;
import com.fastasyncworldedit.core.internal.io.ResettableFileInputStream;
import com.google.common.collect.ImmutableSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV1Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV2Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV2Writer;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A collection of supported clipboard formats.
 */
@SuppressWarnings("removal") //FAWE: suppress JNBT deprecations
public enum BuiltInClipboardFormat implements ClipboardFormat {

    //FAWE start - register fast clipboard io
    FAST_V3("fast", "fawe", "schem") { // For testing purposes

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new FastSchematicReaderV3(inputStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            OutputStream gzip;
            if (outputStream instanceof ParallelGZIPOutputStream || outputStream instanceof GZIPOutputStream) {
                gzip = outputStream;
            } else {
                outputStream = new BufferedOutputStream(outputStream);
                gzip = new ParallelGZIPOutputStream(outputStream);
            }
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            return new FastSchematicWriterV3(nbtStream);
        }

        @Override
        public boolean isFormat(final File file) {
            try (final DataInputStream stream = new DataInputStream(new GZIPInputStream(Files.newInputStream(file.toPath())));
                 final NBTInputStream nbt = new NBTInputStream(stream)) {
                if (stream.readByte() != NBTConstants.TYPE_COMPOUND) {
                    return false;
                }
                stream.skipNBytes(2); // TAG name length ("" = 0), no need to read name as no bytes are written for root tag
                if (stream.readByte() != NBTConstants.TYPE_COMPOUND) {
                    return false;
                }
                stream.skipNBytes(2); // TAG name length ("Schematic" = 9)
                stream.skipNBytes(9); // "Schematic"

                // We can't guarantee the specific order of nbt data, so scan and skip, if required
                do {
                    byte type = stream.readByte();
                    String name = stream.readUTF();
                    if (type == NBTConstants.TYPE_END) {
                        return false;
                    }
                    if (type == NBTConstants.TYPE_INT && name.equals("Version")) {
                        return stream.readInt() == FastSchematicWriterV3.CURRENT_VERSION;
                    }
                    nbt.readTagPayloadLazy(type, 0);
                } while (true);
            } catch (IOException ignored) {
            }
            return false;
        }

        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }
    },
    FAST_V2("fast.2", "fawe.2", "schem.2") {
        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            if (inputStream instanceof FileInputStream) {
                inputStream = new ResettableFileInputStream((FileInputStream) inputStream);
            }
            BufferedInputStream buffered = new BufferedInputStream(inputStream);
            NBTInputStream nbtStream = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(buffered)));
            return new FastSchematicReaderV2(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            OutputStream gzip;
            if (outputStream instanceof ParallelGZIPOutputStream || outputStream instanceof GZIPOutputStream) {
                gzip = outputStream;
            } else {
                outputStream = new BufferedOutputStream(outputStream);
                gzip = new ParallelGZIPOutputStream(outputStream);
            }
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            return new FastSchematicWriterV2(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            try (final DataInputStream stream = new DataInputStream(new GZIPInputStream(Files.newInputStream(file.toPath())));
                 final NBTInputStream nbt = new NBTInputStream(stream)) {
                if (stream.readByte() != NBTConstants.TYPE_COMPOUND) {
                    return false;
                }
                stream.skipNBytes(2); // TAG name length ("Schematic" = 9)
                stream.skipNBytes(9); // "Schematic"

                // We can't guarantee the specific order of nbt data, so scan and skip, if required
                do {
                    byte type = stream.readByte();
                    String name = stream.readUTF();
                    if (type == NBTConstants.TYPE_END) {
                        return false;
                    }
                    if (type == NBTConstants.TYPE_INT && name.equals("Version")) {
                        return stream.readInt() == FastSchematicWriterV2.CURRENT_VERSION;
                    }
                    nbt.readTagPayloadLazy(type, 0);
                } while (true);
            } catch (IOException ignored) {
            }
            return false;
        }

    },
    //FAWE end

    /**
     * The Schematic format used by MCEdit.
     */
    MCEDIT_SCHEMATIC("mcedit", "mce", "schematic") {
        @Override
        public String getPrimaryFileExtension() {
            return "schematic";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new MCEditSchematicReader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            //FAWE start - be a more helpful exception
            throw new IOException("The formats `.schematic`, `.mcedit` and `.mce` are discontinued on versions newer than " +
                    "1.12 and superseded by the sponge schematic implementation known for `.schem` files.");
            //FAWE end
        }

        @Override
        public boolean isFormat(File file) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return name.endsWith(".schematic") || name.endsWith(".mcedit") || name.endsWith(".mce");
        }
    },
    SPONGE_V1_SCHEMATIC("sponge.1") {

        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new SpongeSchematicV1Reader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            throw new IOException("This format does not support saving");
        }

        @Override
        public boolean isFormat(File file) {
            try (NBTInputStream str = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                NamedTag rootTag = str.readNamedTag();
                if (!rootTag.getName().equals("Schematic")) {
                    return false;
                }
                CompoundTag schematicTag = (CompoundTag) rootTag.getTag();

                // Check
                Map<String, Tag> schematic = schematicTag.getValue();
                Tag versionTag = schematic.get("Version");
                if (!(versionTag instanceof IntTag) || ((IntTag) versionTag).getValue() != 1) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }
    },

    /**
     * @deprecated Slow, resource intensive, but sometimes safer than using the recommended
     *         {@link BuiltInClipboardFormat#FAST}.
     *         Avoid using with any large schematics/clipboards for reading/writing.
     */
    @Deprecated
    SPONGE_V2_SCHEMATIC("slow.2", "safe.2", "sponge.2") {
        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new SpongeSchematicV2Reader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            NBTOutputStream nbtStream = new NBTOutputStream(new GZIPOutputStream(outputStream));
            return new SpongeSchematicV2Writer(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            try (NBTInputStream str = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                NamedTag rootTag = str.readNamedTag();
                if (!rootTag.getName().equals("Schematic")) {
                    return false;
                }
                CompoundTag schematicTag = (CompoundTag) rootTag.getTag();

                // Check
                Map<String, Tag> schematic = schematicTag.getValue();
                Tag versionTag = schematic.get("Version");
                if (!(versionTag instanceof IntTag) || ((IntTag) versionTag).getValue() != 2) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }
    },
    SPONGE_V3_SCHEMATIC("sponge.3", "slow", "safe") {
        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new SpongeSchematicV3Reader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            NBTOutputStream nbtStream = new NBTOutputStream(new GZIPOutputStream(outputStream));
            return new SpongeSchematicV3Writer(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            try (NBTInputStream str = new NBTInputStream(new GZIPInputStream(new FileInputStream(file)))) {
                NamedTag rootTag = str.readNamedTag();
                CompoundTag rootCompoundTag = (CompoundTag) rootTag.getTag();
                if (!rootCompoundTag.containsKey("Schematic")) {
                    return false;
                }
                Tag schematicTag = rootCompoundTag.getValue()
                        .get("Schematic");
                if (!(schematicTag instanceof CompoundTag)) {
                    return false;
                }

                // Check
                Map<String, Tag> schematic = ((CompoundTag) schematicTag).getValue();
                Tag versionTag = schematic.get("Version");
                if (!(versionTag instanceof IntTag) || ((IntTag) versionTag).getValue() != 3) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }

            return true;
        }
    },
    //FAWE start - recover schematics with bad entity data & register other clipboard formats
    BROKENENTITY("brokenentity", "legacyentity", "le", "be", "brokenentities", "legacyentities") {
        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            if (inputStream instanceof FileInputStream) {
                inputStream = new ResettableFileInputStream((FileInputStream) inputStream);
            }
            BufferedInputStream buffered = new BufferedInputStream(inputStream);
            NBTInputStream nbtStream = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(buffered)));
            FastSchematicReaderV2 reader = new FastSchematicReaderV2(nbtStream);
            reader.setBrokenEntities(true);
            return reader;
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            OutputStream gzip;
            if (outputStream instanceof ParallelGZIPOutputStream || outputStream instanceof GZIPOutputStream) {
                gzip = outputStream;
            } else {
                outputStream = new BufferedOutputStream(outputStream);
                gzip = new ParallelGZIPOutputStream(outputStream);
            }
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            FastSchematicWriterV2 writer = new FastSchematicWriterV2(nbtStream);
            writer.setBrokenEntities(true);
            return writer;
        }

        @Override
        public boolean isFormat(File file) {
            return false;
        }

    },

    /**
     * The structure block format:
     * http://minecraft.gamepedia.com/Structure_block_file_format
     */
    MINECRAFT_STRUCTURE("structure") {
        @Override
        public String getPrimaryFileExtension() {
            return "nbt";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            inputStream = new BufferedInputStream(inputStream);
            NBTInputStream nbtStream = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(inputStream)));
            return new MinecraftStructure(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            outputStream = new BufferedOutputStream(outputStream);
            OutputStream gzip = new ParallelGZIPOutputStream(outputStream);
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            return new MinecraftStructure(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return name.endsWith(".nbt");
        }
    },

    /**
     * Isometric PNG writer. Only supports solid, full-cube blocks and creates a view of the clipboard looking FROM the south
     * east of the clipboard
     */
    PNG("png", "image") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) {
            return null;
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return new PNGWriter(new BufferedOutputStream(outputStream));
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".png");
        }

        @Override
        public String getPrimaryFileExtension() {
            return "png";
        }
    };
    //FAWE end

    /**
     * For backwards compatibility, this points to the Sponge Schematic Specification (Version 2)
     * format. This should not be used going forwards.
     *
     * @deprecated Use {@link #SPONGE_V2_SCHEMATIC} or {@link #SPONGE_V3_SCHEMATIC}
     */
    @Deprecated
    public static final BuiltInClipboardFormat SPONGE_SCHEMATIC = SPONGE_V2_SCHEMATIC;

    //FAWE start
    /**
     * For backwards compatibility, this points to the fast implementation of the Sponge Schematic Specification (Version 2)
     * format. This should not be used going forwards.
     *
     * @deprecated Use {@link #FAST_V2} or {@link #FAST_V3}
     */
    @Deprecated
    public static final BuiltInClipboardFormat FAST = FAST_V2;
    //FAWE end

    private final ImmutableSet<String> aliases;

    BuiltInClipboardFormat(String... aliases) {
        this.aliases = ImmutableSet.copyOf(aliases);
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public Set<String> getAliases() {
        return this.aliases;
    }

    @Override
    public Set<String> getFileExtensions() {
        return ImmutableSet.of(getPrimaryFileExtension());
    }

}
