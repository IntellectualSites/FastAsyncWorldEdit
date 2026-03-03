package com.fastasyncworldedit.nukkit.mapping;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Java Edition block state, consisting of an identifier and properties.
 * Uses FNV-1a 32-bit hash for fast lookup in mapping tables.
 */
public class JeBlockState {

    private final String identifier;
    private final TreeMap<String, String> properties;
    private int hash = Integer.MAX_VALUE;

    private JeBlockState(String data) {
        int braceIndex = data.indexOf('{');
        if (braceIndex != -1) {
            data = data.substring(0, braceIndex);
        }

        String[] strings = data
                .replace("[", ",")
                .replace("]", ",")
                .replace(" ", "")
                .split(",");

        this.identifier = strings[0];
        this.properties = new TreeMap<>();
        if (strings.length > 1) {
            for (int i = 1; i < strings.length; i++) {
                final String tmp = strings[i];
                if (tmp.isEmpty()) {
                    continue;
                }
                final int index = tmp.indexOf("=");
                properties.put(tmp.substring(0, index), tmp.substring(index + 1));
            }
        }
    }

    private JeBlockState(String identifier, TreeMap<String, String> properties) {
        this.identifier = identifier;
        this.properties = properties;
    }

    public static JeBlockState fromString(String data) {
        return new JeBlockState(data);
    }

    public static JeBlockState create(String identifier, TreeMap<String, String> properties) {
        return new JeBlockState(identifier, properties);
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPropertyValue(String key) {
        return properties.get(key);
    }

    public TreeMap<String, String> getProperties() {
        return properties;
    }

    /**
     * Complete missing properties using the provided default properties.
     */
    public void completeMissingProperties(Map<String, String> defaultProperties) {
        if (defaultProperties == null || properties.size() == defaultProperties.size()) {
            return;
        }
        for (Map.Entry<String, String> entry : defaultProperties.entrySet()) {
            properties.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Build the canonical string representation: "identifier;key=value;key=value;..."
     * Properties are sorted by key (TreeMap guarantees this).
     */
    public String toCanonicalString() {
        StringBuilder builder = new StringBuilder(identifier).append(";");
        properties.forEach((k, v) -> builder.append(k).append("=").append(v).append(";"));
        return builder.toString();
    }

    /**
     * Get FNV-1a 32-bit hash of the canonical string representation.
     */
    public int getHash() {
        if (hash == Integer.MAX_VALUE) {
            hash = fnv1a32(toCanonicalString().getBytes());
        }
        return hash;
    }

    @Override
    public String toString() {
        if (properties.isEmpty()) {
            return identifier;
        }
        StringBuilder sb = new StringBuilder(identifier).append("[");
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.append("]").toString();
    }

    private static int fnv1a32(byte[] data) {
        int hash = 0x811c9dc5;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= 0x01000193;
        }
        return hash;
    }

}
