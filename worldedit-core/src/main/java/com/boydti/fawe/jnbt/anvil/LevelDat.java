package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkArgument;

public class LevelDat {
    private final File file;
    private CompoundTag tag;

    public LevelDat(File file) {
        checkArgument(file.exists());
        this.file = file;
    }

    public void load() throws IOException {
        try (NBTInputStream nis = new NBTInputStream(new FastBufferedInputStream(new GZIPInputStream(new FastBufferedInputStream(new FileInputStream(file)))))) {
            this.tag = (CompoundTag) nis.readNamedTag().getTag();
        }
    }

    public void save() throws IOException {
        if (this.tag != null) {
            try (NBTOutputStream nos = new NBTOutputStream(new FastBufferedOutputStream(new GZIPOutputStream(new FastBufferedOutputStream(new FileOutputStream(file)))))) {
                nos.writeNamedTag("", tag);
            }
        }
    }

    public CompoundTag getTag() {
        return tag;
    }
}
