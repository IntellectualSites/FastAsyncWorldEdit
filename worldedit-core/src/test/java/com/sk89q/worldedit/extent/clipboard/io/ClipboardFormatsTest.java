package com.sk89q.worldedit.extent.clipboard.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;

class ClipboardFormatsTest {

    @Test
    void findByFile() {
        Assertions.assertSame(
                BuiltInClipboardFormat.SPONGE_V1_SCHEMATIC,
                ClipboardFormats.findByFile(getTestSchematic("sponge1.schem"))
        );
        Assertions.assertSame(
                BuiltInClipboardFormat.FAST_V2,
                ClipboardFormats.findByFile(getTestSchematic("sponge2.schem"))
        );
        Assertions.assertSame(
                BuiltInClipboardFormat.FAST_V3,
                ClipboardFormats.findByFile(getTestSchematic("sponge3.schem"))
        );
        Assertions.assertSame(
                BuiltInClipboardFormat.MCEDIT_SCHEMATIC,
                ClipboardFormats.findByFile(getTestSchematic("mcedit.mce"))
        );
        Assertions.assertSame(
                BuiltInClipboardFormat.MINECRAFT_STRUCTURE,
                ClipboardFormats.findByFile(getTestSchematic("minecraft_structure.nbt"))
        );
        Assertions.assertSame(
                BuiltInClipboardFormat.PNG,
                ClipboardFormats.findByFile(getTestSchematic("1x1.png"))
        );

        Assertions.assertNull(
                ClipboardFormats.findByFile(getTestSchematic("custom_format.xyz"))
        );
        ClipboardFormats.registerClipboardFormat(new CustomTestingClipboardFormat());
        Assertions.assertInstanceOf(
                CustomTestingClipboardFormat.class,
                ClipboardFormats.findByFile(getTestSchematic("custom_format.xyz"))
        );
    }

    private static File getTestSchematic(String name) {
        return Path.of("src", "test", "resources", "fastasyncworldedit", "schematics", name).toFile();
    }

    private static final class CustomTestingClipboardFormat implements ClipboardFormat {

        @Override
        public String getName() {
            return "Custom Testing Format";
        }

        @Override
        public boolean isFormat(final File file) {
            return file.getName().endsWith(".xyz");
        }

        @Override
        public Set<String> getAliases() {
            return Set.of();
        }

        @Override
        public ClipboardReader getReader(final InputStream inputStream) {
            return null;
        }

        @Override
        public ClipboardWriter getWriter(final OutputStream outputStream) {
            return null;
        }

        @Override
        public String getPrimaryFileExtension() {
            return "";
        }

        @Override
        public Set<String> getFileExtensions() {
            return Set.of();
        }

        @Override
        public Set<String> getExplicitFileExtensions() {
            return Set.of();
        }

    }

}
