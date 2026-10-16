package com.fastasyncworldedit.core.configuration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes the YAML files behind the configuration records.
 * <p>
 * Files are read with SnakeYAML and written by {@link Writer}.
 */
final class ConfigYaml {

    private ConfigYaml() {
    }

    /**
     * Parses a file into nested maps with string keys.
     *
     * @throws YAMLException if the file is not valid YAML or its top level is not a mapping
     */
    static Map<String, Object> read(Path file) throws IOException {
        Object document;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            document = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
        if (document == null) {
            return new LinkedHashMap<>();
        }
        if (!(document instanceof Map<?, ?> map)) {
            throw new YAMLException("The top level of " + file.getFileName() + " is not a mapping");
        }
        return stringKeyed(map);
    }

    private static Map<String, Object> stringKeyed(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            result.put(String.valueOf(entry.getKey()), value instanceof Map<?, ?> child ? stringKeyed(child) : value);
        }
        return result;
    }

    /**
     * Writes the content to the file unless the file already holds exactly that content.
     */
    static void write(Path file, Writer content) throws IOException {
        BasicFileAttributes existing;
        try {
            existing = Files.readAttributes(file, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            existing = null;
        }
        // Only a file of the same size can hold the same content, so a changed file is not read first
        if (existing != null && existing.isRegularFile() && existing.size() == content.length()
                && content.matches(Files.readAllBytes(file))) {
            return;
        }
        // Written in place so the file keeps its owner, permissions and any link it is reached through
        try {
            writeBytes(file, content);
        } catch (NoSuchFileException e) {
            Path parent = file.toAbsolutePath().getParent();
            if (parent == null || Files.isDirectory(parent)) {
                throw e;
            }
            Files.createDirectories(parent);
            writeBytes(file, content);
        }
    }

    private static void writeBytes(Path file, Writer content) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file)) {
            content.writeTo(stream);
        }
    }

    /**
     * Copies a file that could not be read next to itself so it is not lost when the defaults are written.
     *
     * @return the copy
     */
    static Path backup(Path file) throws IOException {
        Path backup = file.resolveSibling(file.getFileName() + "_broken");
        for (int i = 0; Files.exists(backup); i++) {
            backup = file.resolveSibling(file.getFileName() + "_broken_" + i);
        }
        return Files.copy(file, backup);
    }

    /**
     * Builds a file in block style, indented by two spaces, with text always in double quotes, directly as UTF-8.
     * <p>
     * The configuration only holds sections, booleans, whole numbers, text and lists of text, which is little enough to
     * write directly. Going through SnakeYAML's node tree and emitter made saving slower than the old hand-written output.
     * The bytes are compared with the file and written from the same buffer, so the content is never copied.
     */
    static final class Writer {

        private static final byte[] LINE_BREAK = DumperOptions.LineBreak.getPlatformLineBreak().getString()
                .getBytes(StandardCharsets.US_ASCII);
        private static final Resolver RESOLVER = new Resolver();
        private static final byte[] HEX = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        // Keys and comments come from the records and the limit group names, which are few and written on every save, so
        // each is checked and encoded once. A key that needs quotes maps to QUOTED.
        private static final Map<String, byte[]> KEYS = new ConcurrentHashMap<>();
        private static final Map<String, byte[]> COMMENTS = new ConcurrentHashMap<>();
        private static final byte[] QUOTED = new byte[0];

        private byte[] bytes = new byte[16 * 1024];
        private int length;

        Writer blankLine() {
            lineBreak();
            return this;
        }

        Writer comments(int indent, List<String> comments) {
            for (String comment : comments) {
                indent(indent);
                put('#');
                if (!comment.isEmpty()) {
                    put(' ');
                    copy(COMMENTS.computeIfAbsent(comment, text -> text.getBytes(StandardCharsets.UTF_8)));
                }
                lineBreak();
            }
            return this;
        }

        /**
         * Starts an entry. It must be followed by {@link #scalar}, {@link #list} or {@link #section}.
         */
        Writer key(int indent, String key) {
            indent(indent);
            byte[] plain = KEYS.computeIfAbsent(key, name -> isPlain(name) ? name.getBytes(StandardCharsets.UTF_8) : QUOTED);
            if (plain == QUOTED) {
                quoted(key);
            } else {
                copy(plain);
            }
            put(':');
            return this;
        }

        /**
         * Ends an entry with a boolean, a whole number, text or null.
         */
        void scalar(@Nullable Object value) {
            put(' ');
            if (value == null) {
                text("null");
            } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
                text(value.toString());
            } else {
                quoted(value.toString());
            }
            lineBreak();
        }

        /**
         * Ends an entry with a list, whose items go at the indent of its key.
         */
        void list(int indent, List<String> values) {
            if (values.isEmpty()) {
                text(" []");
                lineBreak();
                return;
            }
            lineBreak();
            for (String value : values) {
                indent(indent);
                text("- ");
                quoted(value);
                lineBreak();
            }
        }

        /**
         * Ends an entry whose value is a section. Its entries follow, indented two more spaces.
         */
        void section(boolean empty) {
            if (empty) {
                text(" {}");
            }
            lineBreak();
        }

        /**
         * {@return if the file holds exactly what was written}
         */
        boolean matches(byte[] content) {
            return Arrays.equals(bytes, 0, length, content, 0, content.length);
        }

        int length() {
            return length;
        }

        void writeTo(OutputStream stream) throws IOException {
            stream.write(bytes, 0, length);
        }

        @Override
        public String toString() {
            return new String(bytes, 0, length, StandardCharsets.UTF_8);
        }

        /**
         * {@return if a key can be written as is}
         * That is a simple name which YAML reads back as text: {@code on}, {@code 123} or {@code null} need quotes.
         */
        private static boolean isPlain(String key) {
            if (key.isEmpty() || key.charAt(0) == '-') {
                return false;
            }
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_')) {
                    return false;
                }
            }
            return RESOLVER.resolve(NodeId.scalar, key, true) == Tag.STR;
        }

        /**
         * Writes text in double quotes, escaping what YAML requires and what would not survive being written as is.
         */
        private void quoted(String value) {
            // At most six bytes per character, for a lone surrogate written as a four digit escape
            reserve(value.length() * 6 + 2);
            raw('"');
            for (int i = 0; i < value.length(); ) {
                char ch = value.charAt(i);
                if (ch >= 0x20 && ch < 0x7F && ch != '"' && ch != '\\') {
                    raw(ch);
                    i++;
                    continue;
                }
                int c = value.codePointAt(i);
                i += Character.charCount(c);
                switch (c) {
                    case '"' -> escape('"');
                    case '\\' -> escape('\\');
                    case 0 -> escape('0');
                    case 0x07 -> escape('a');
                    case '\b' -> escape('b');
                    case '\t' -> escape('t');
                    case '\n' -> escape('n');
                    case 0x0B -> escape('v');
                    case '\f' -> escape('f');
                    case '\r' -> escape('r');
                    case 0x1B -> escape('e');
                    case 0x85 -> escape('N');
                    case 0xA0 -> escape('_');
                    case 0x2028 -> escape('L');
                    case 0x2029 -> escape('P');
                    default -> {
                        if (isPrintable(c)) {
                            codePoint(c);
                        } else if (c <= 0xFF) {
                            hex('x', c, 2);
                        } else if (c <= 0xFFFF) {
                            hex('u', c, 4);
                        } else {
                            hex('U', c, 8);
                        }
                    }
                }
            }
            raw('"');
        }

        /**
         * {@return if YAML allows the character unescaped} Lone surrogates and the byte order mark are escaped.
         */
        private static boolean isPrintable(int c) {
            return c >= 0x20 && c <= 0x7E || c >= 0xA1 && c <= 0xD7FF || c >= 0xE000 && c <= 0xFFFD && c != 0xFEFF
                    || c >= 0x10000 && c <= 0x10FFFF;
        }

        private void escape(char c) {
            raw('\\');
            raw(c);
        }

        private void hex(char kind, int c, int digits) {
            escape(kind);
            for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
                raw(HEX[(c >> shift) & 0xF]);
            }
        }

        /**
         * Writes text as is, as UTF-8. A lone surrogate becomes {@code ?}, as {@link String#getBytes} would make it.
         */
        private void text(String text) {
            // At most three bytes per character, as a character outside the BMP takes two
            reserve(text.length() * 3);
            for (int i = 0; i < text.length(); ) {
                char c = text.charAt(i);
                if (c < 0x80) {
                    raw(c);
                    i++;
                } else {
                    int codePoint = text.codePointAt(i);
                    i += Character.charCount(codePoint);
                    codePoint(codePoint <= 0xFFFF && Character.isSurrogate((char) codePoint) ? '?' : codePoint);
                }
            }
        }

        /**
         * Writes a character as UTF-8. The caller reserves the space.
         */
        private void codePoint(int c) {
            if (c < 0x80) {
                raw(c);
            } else if (c < 0x800) {
                raw(0xC0 | c >> 6);
                raw(0x80 | c & 0x3F);
            } else if (c < 0x10000) {
                raw(0xE0 | c >> 12);
                raw(0x80 | c >> 6 & 0x3F);
                raw(0x80 | c & 0x3F);
            } else {
                raw(0xF0 | c >> 18);
                raw(0x80 | c >> 12 & 0x3F);
                raw(0x80 | c >> 6 & 0x3F);
                raw(0x80 | c & 0x3F);
            }
        }

        private void indent(int indent) {
            reserve(indent);
            Arrays.fill(bytes, length, length + indent, (byte) ' ');
            length += indent;
        }

        private void lineBreak() {
            copy(LINE_BREAK);
        }

        private void copy(byte[] encoded) {
            reserve(encoded.length);
            System.arraycopy(encoded, 0, bytes, length, encoded.length);
            length += encoded.length;
        }

        private void put(int b) {
            reserve(1);
            raw(b);
        }

        private void reserve(int count) {
            if (length + count > bytes.length) {
                bytes = Arrays.copyOf(bytes, Math.max(bytes.length * 2, length + count));
            }
        }

        /**
         * Writes a byte into space already reserved.
         */
        private void raw(int b) {
            bytes[length++] = (byte) b;
        }

    }

}
