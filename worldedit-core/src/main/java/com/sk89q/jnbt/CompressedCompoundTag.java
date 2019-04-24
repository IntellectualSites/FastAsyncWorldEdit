package com.sk89q.jnbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class CompressedCompoundTag<T> extends CompoundTag {
    private T in;

    public CompressedCompoundTag(T in) {
        super(new HashMap<>());
        this.in = in;
    }

    @Override
    public Map<String, Tag> getValue() {
        if (in != null) decompress();
        return super.getValue();
    }

    public abstract DataInputStream adapt(T src) throws IOException;

    public T getSource() {
        return in;
    }

    private void decompress() {
        try (NBTInputStream nbtIn = new NBTInputStream(adapt(in))) {
            in = null;
            CompoundTag tag = (CompoundTag) nbtIn.readTag();
            Map<String, Tag> value = tag.getValue();
            Map<String, Tag> raw = super.getValue();
            for (Map.Entry<String, Tag> entry : value.entrySet()) {
                raw.put(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
