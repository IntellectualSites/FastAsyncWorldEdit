package com.boydti.fawe.jnbt.anvil.history;

import java.io.File;

public interface IAnvilHistory {
    default boolean addFileChange(File originalMCAFile) {
        return originalMCAFile.delete();
    }
}
