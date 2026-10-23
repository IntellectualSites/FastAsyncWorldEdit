package cn.nukkit.nbt.tag;

import java.util.HashMap;

public class CompoundTag extends HashMap<String, Object> {

    public void putString(String key, String value) {
        put(key, value);
    }

    public String getString(String key) {
        Object value = get(key);
        return value == null ? "" : value.toString();
    }
}
