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
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV1Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV2Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV2Writer;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Reader;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import org.anarres.parallelgzip.ParallelGZIPOutputStream;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.stream.LinReadOptions;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinRootEntry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A collection of supported clipboard formats.
 */
@SuppressWarnings("removal") //FAWE: suppress JNBT deprecations
public enum BuiltInClipboardFormat implements ClipboardFormat {

    //FAWE start - register fast clipboard io
    FAST_V3("fast", "fawe", "schem") {
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
        public boolean isFormat(final InputStream inputStream) {
            try (final DataInputStream stream = new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(inputStream)));
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
        public boolean isFormat(File file) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".schematic") || name.endsWith(".mcedit") || name.endsWith(".mce")) {
                return false;
            }
            return super.isFormat(file);
        }

        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of("schem3", "sponge3", "fast3");
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
        public boolean isFormat(InputStream inputStream) {
            return detectOldSpongeSchematic(inputStream, FastSchematicWriterV2.CURRENT_VERSION);
        }

        @Override
        public boolean isFormat(File file) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".schematic") || name.endsWith(".mcedit") || name.endsWith(".mce")) {
                return false;
            }
            return super.isFormat(file);
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of("schem2", "sponge2", "fast2");
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
            if (!name.endsWith(".schematic") && !name.endsWith(".mcedit") && !name.endsWith(".mce")) {
                return false;
            }
            return super.isFormat(file);
        }

        @Override
        public boolean isFormat(InputStream inputStream) {
            LinRootEntry rootEntry;
            try {
                DataInputStream stream = new DataInputStream(new GZIPInputStream(inputStream));
                rootEntry = LinBinaryIO.readUsing(stream, LEGACY_OPTIONS, LinRootEntry::readFrom);
            } catch (Exception e) {
                return false;
            }
            if (!rootEntry.name().equals("Schematic")) {
                return false;
            }
            return rootEntry.value().value().containsKey("Materials");
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of("mcedit", "schem1", "sponge1", "fast1");
        }
    },
    SPONGE_V1_SCHEMATIC("sponge.1") {
        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new SpongeSchematicV1Reader(LinBinaryIO.read(
                new DataInputStream(new GZIPInputStream(inputStream)), LEGACY_OPTIONS
            ));
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            throw new IOException("This format does not support saving");
        }

        @Override
        public boolean isFormat(InputStream inputStream) {
            return detectOldSpongeSchematic(inputStream, 1);
        }

        @Override
        public boolean isFormat(File file) {
            return MCEDIT_SCHEMATIC.isFormat(file);
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Collections.emptySet();
        }
    },

    /**
     * @deprecated Slow, resource intensive, but sometimes safer than using the recommended
     *         {@link BuiltInClipboardFormat#FAST}.
     *         Avoid using with any large schematics/clipboards for reading/writing.
     */
    @Deprecated
    SPONGE_V2_SCHEMATIC("slow.2", "safe.2", "sponge.2") { // FAWE - edit aliases for fast

        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new SpongeSchematicV2Reader(LinBinaryIO.read(
                new DataInputStream(new GZIPInputStream(inputStream)), LEGACY_OPTIONS
            ));
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return new SpongeSchematicV2Writer(new DataOutputStream(new GZIPOutputStream(outputStream)));
        }

        @Override
        public boolean isFormat(InputStream inputStream) {
            return detectOldSpongeSchematic(inputStream, 2);
        }

        @Override
        public boolean isFormat(File file) {
            return FAST_V2.isFormat(file);
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Collections.emptySet();
        }
    },
    SPONGE_V3_SCHEMATIC("sponge.3", "slow", "safe") { // FAWE - edit aliases for fast

        @Override
        public String getPrimaryFileExtension() {
            return "schem";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new SpongeSchematicV3Reader(LinBinaryIO.read(new DataInputStream(new GZIPInputStream(inputStream))));
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return new SpongeSchematicV3Writer(new DataOutputStream(new GZIPOutputStream(outputStream)));
        }

        @Override
        public boolean isFormat(File file) {
            //FAWE start - delegate to stream-based isFormat approach of fast impl
            return FAST_V3.isFormat(file);
            //FAWE end
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Collections.emptySet();
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

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Collections.emptySet();
        }
    },

    /**
     * The structure block format:
     * <a href="https://minecraft.wiki/w/Structure_file">Structure file - Minecraft Wiki</a>
     */
    MINECRAFT_STRUCTURE("structure") {
        @Override
        public String getPrimaryFileExtension() {
            return "nbt";
        }

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            inputStream = new BufferedInputStream(inputStream);
            return new MinecraftStructure(new DataInputStream(new GZIPInputStream(inputStream)));
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            outputStream = new BufferedOutputStream(outputStream);
            OutputStream gzip = new ParallelGZIPOutputStream(outputStream);
            return new MinecraftStructure(new DataOutputStream(new BufferedOutputStream(gzip)));
        }

        @Override
        public boolean isFormat(InputStream inputStream) {
            try (final DataInputStream stream = new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(inputStream)));
                 final NBTInputStream nbt = new NBTInputStream(stream)) {
                if (stream.readByte() != NBTConstants.TYPE_COMPOUND) {
                    return false;
                }
                NamedTag namedTag = nbt.readNamedTag();
                if (!namedTag.getName().isEmpty()) {
                    return false;
                }

                // We can't guarantee the specific order of nbt data, so scan and skip, if required
                do {
                    byte type = stream.readByte();
                    String name = stream.readUTF();
                    if (type == NBTConstants.TYPE_END) {
                        return false;
                    }
                    if (type == NBTConstants.TYPE_LIST && name.equals("size")) {
                        return true;
                    }
                    nbt.readTagPayloadLazy(type, 0);
                } while (true);
            } catch (IOException ignored) {
            }
            return false;
        }

        @Override
        public boolean isFormat(final File file) {
            return file.getName().toLowerCase(Locale.ROOT).endsWith(".nbt") && super.isFormat(file);
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of("nbt");
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

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of("png");
        }
    };
    //FAWE end

    private static boolean detectOldSpongeSchematic(InputStream inputStream, int version) {
        //FAWE start - dont utilize linbus - WorldEdit approach is not really streamed
        try (final DataInputStream stream = new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(inputStream)));
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
                    return stream.readInt() == version;
                }
                nbt.readTagPayloadLazy(type, 0);
            } while (true);
        } catch (IOException ignored) {
        }
        return false;
    }

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

    private static final LinReadOptions LEGACY_OPTIONS = LinReadOptions.builder().allowNormalUtf8Encoding(true).build();

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
