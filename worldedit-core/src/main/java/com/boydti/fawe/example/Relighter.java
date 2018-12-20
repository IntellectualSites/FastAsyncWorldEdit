package com.boydti.fawe.example;

public interface Relighter {
    boolean addChunk(int cx, int cz, byte[] skipReason, int bitmask);

    void addLightUpdate(int x, int y, int z);

    void fixLightingSafe(boolean sky);

    default void removeAndRelight(boolean sky) {
        removeLighting();
        fixLightingSafe(sky);
    }

    void clear();

    void removeLighting();

    void fixBlockLighting();

    void fixSkyLighting();

    boolean isEmpty();

    public static class SkipReason {
        public static final byte NONE = 0;
        public static final byte AIR = 1;
        public static final byte SOLID = 2;
    }
}