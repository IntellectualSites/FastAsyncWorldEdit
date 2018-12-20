package com.boydti.fawe.example;

public class NullRelighter implements Relighter {

    public static NullRelighter INSTANCE = new NullRelighter();

    private NullRelighter() {
    }

    @Override
    public boolean addChunk(int cx, int cz, byte[] fix, int bitmask) {
        return false;
    }

    @Override
    public void addLightUpdate(int x, int y, int z) {

    }

    @Override
    public void fixLightingSafe(boolean sky) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void removeLighting() {

    }

    @Override
    public void fixBlockLighting() {

    }

    @Override
    public void fixSkyLighting() {

    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
