package com.boydti.fawe.object.collection;

public class VariableThreadLocal extends CleanableThreadLocal<byte[]> {
    public VariableThreadLocal() {
        super(() -> null);
    }

    public byte[] get(int size) {
        byte[] existing = get();
        if (existing == null || existing.length < size) {
            int padded = ((size + 4095) / 4096) * 4096;
            existing = new byte[padded];
            set(existing);
        }
        return existing;
    }
}
