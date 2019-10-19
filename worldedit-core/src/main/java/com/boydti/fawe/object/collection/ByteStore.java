package com.boydti.fawe.object.collection;

public class ByteStore extends CleanableThreadLocal<byte[]> {
    private final int size;

    public ByteStore(int size) {
        this.size = size;
    }

    @Override
    public byte[] init() {
        return new byte[size];
    }
}
