package com.boydti.fawe.object.clipboard;

import java.util.Arrays;
import java.util.HashSet;

public abstract class AbstractClipboardFormat implements IClipboardFormat {
    private final String name;
    private final HashSet<String> aliases;

    public AbstractClipboardFormat(String name, String... aliases) {
        this.name = name;
        this.aliases = new HashSet<>(Arrays.asList(aliases));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashSet<String> getAliases() {
        return aliases;
    }
}
