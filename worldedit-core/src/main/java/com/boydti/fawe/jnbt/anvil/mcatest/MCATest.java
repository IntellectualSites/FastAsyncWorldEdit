package com.boydti.fawe.jnbt.anvil.mcatest;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;

import java.io.File;
import java.io.IOException;

public class MCATest {
    public MCATest() throws IOException {
        File file = new File("plugins/FastAsyncWorldEdit/tobitower.schematic");
        Clipboard loaded = ClipboardFormats.findByFile(file).load(file);
    }
}
