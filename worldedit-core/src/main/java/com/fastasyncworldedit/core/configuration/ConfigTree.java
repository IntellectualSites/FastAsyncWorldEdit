package com.fastasyncworldedit.core.configuration;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dotted-path access to a parsed configuration file, used to move old options to their current place before the file is
 * mapped onto the configuration records.
 */
final class ConfigTree {

    private final Map<String, Object> root;

    ConfigTree(Map<String, Object> root) {
        this.root = root;
    }

    @Nullable
    Object get(String path) {
        Map<String, Object> parent = parent(path, false);
        return parent == null ? null : parent.get(leaf(path));
    }

    @Nullable
    Object remove(String path) {
        Map<String, Object> parent = parent(path, false);
        return parent == null ? null : parent.remove(leaf(path));
    }

    /**
     * Sets a value unless the file already has one. Sections on the way are created; nothing is written if something
     * other than a section is in the way.
     */
    void putIfAbsent(String path, Object value) {
        Map<String, Object> parent = parent(path, true);
        if (parent != null) {
            parent.putIfAbsent(leaf(path), value);
        }
    }

    /**
     * {@return the section at the path, or null if there is none}
     */
    @Nullable
    Map<String, Object> section(String path) {
        return asSection(get(path));
    }

    private static String leaf(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    @Nullable
    private Map<String, Object> parent(String path, boolean create) {
        Map<String, Object> node = root;
        int start = 0;
        for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', start)) {
            String key = path.substring(start, dot);
            Object child = node.get(key);
            if (child == null && create) {
                child = new LinkedHashMap<String, Object>();
                node.put(key, child);
            }
            node = asSection(child);
            if (node == null) {
                return null;
            }
            start = dot + 1;
        }
        return node;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asSection(@Nullable Object value) {
        // ConfigYaml.read only produces maps with string keys
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

}
