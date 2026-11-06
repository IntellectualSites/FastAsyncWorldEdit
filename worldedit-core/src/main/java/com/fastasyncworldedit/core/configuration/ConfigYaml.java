package com.fastasyncworldedit.core.configuration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the YAML files behind the configuration records with SnakeYAML.
 * <p>
 * Files are written from a tree of SnakeYAML nodes, as that is how SnakeYAML writes comments: in block style, indented
 * by two spaces, with text in double quotes and long text kept on one line.
 */
final class ConfigYaml {

    private static final DumperOptions OPTIONS = new DumperOptions();

    static {
        OPTIONS.setIndent(2);
        OPTIONS.setProcessComments(true);
        OPTIONS.setSplitLines(false);
        OPTIONS.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
    }

    // Can be shared, as serialize only reads it and makes a new emitter for each call
    private static final Yaml DUMPER = new Yaml(OPTIONS);

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
        for (var entry : map.entrySet()) {
            Object value = entry.getValue();
            result.put(String.valueOf(entry.getKey()), value instanceof Map<?, ?> child ? stringKeyed(child) : value);
        }
        return result;
    }

    /**
     * {@return a file holding the entries, as UTF-8}
     */
    static byte[] dump(List<NodeTuple> entries) {
        StringWriter out = new StringWriter(16 * 1024);
        DUMPER.serialize(new MappingNode(Tag.MAP, entries, DumperOptions.FlowStyle.BLOCK), out);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Writes the content to the file unless the file already holds exactly that content.
     */
    static void write(Path file, byte[] content) throws IOException {
        BasicFileAttributes existing;
        try {
            existing = Files.readAttributes(file, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            existing = null;
        }
        // Only a file of the same size can hold the same content, so a changed file is not read first
        if (existing != null && existing.isRegularFile() && existing.size() == content.length
                && Arrays.equals(content, Files.readAllBytes(file))) {
            return;
        }
        // Written in place so the file keeps its owner, permissions and any link it is reached through
        try {
            Files.write(file, content);
        } catch (NoSuchFileException e) {
            Path parent = file.toAbsolutePath().getParent();
            if (parent == null || Files.isDirectory(parent)) {
                throw e;
            }
            Files.createDirectories(parent);
            Files.write(file, content);
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
     * {@return a key with its comments above it}
     *
     * @param blankLine if an empty line should be left above the comments
     */
    static Node key(String name, List<String> comments, boolean blankLine) {
        Node key = new ScalarNode(Tag.STR, name, null, null, DumperOptions.ScalarStyle.PLAIN);
        List<CommentLine> lines = new ArrayList<>(comments.size() + 1);
        if (blankLine) {
            lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
        }
        for (String comment : comments) {
            lines.add(new CommentLine(null, null, comment.isEmpty() ? "" : " " + comment, CommentType.BLOCK));
        }
        key.setBlockComments(lines);
        return key;
    }

    /**
     * {@return a boolean, a whole number, text or null}
     */
    static Node scalar(@Nullable Object value) {
        return switch (value) {
            case null -> plain(Tag.NULL, "null");
            case Boolean b -> plain(Tag.BOOL, b.toString());
            case Integer i -> plain(Tag.INT, i.toString());
            case Long l -> plain(Tag.INT, l.toString());
            default -> new ScalarNode(Tag.STR, value.toString(), null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        };
    }

    private static Node plain(Tag tag, String value) {
        return new ScalarNode(tag, value, null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    /**
     * {@return a list of text}
     */
    static Node list(List<String> values) {
        List<Node> items = new ArrayList<>(values.size());
        for (String value : values) {
            items.add(scalar(value));
        }
        return new SequenceNode(Tag.SEQ, items, flowIfEmpty(values));
    }

    /**
     * {@return a section holding the entries}
     */
    static Node section(List<NodeTuple> entries) {
        return new MappingNode(Tag.MAP, entries, flowIfEmpty(entries));
    }

    /**
     * {@return the style of a list or section} An empty one is written as {@code []} or {@code {}}.
     */
    private static DumperOptions.FlowStyle flowIfEmpty(List<?> items) {
        return items.isEmpty() ? DumperOptions.FlowStyle.FLOW : DumperOptions.FlowStyle.BLOCK;
    }

}
