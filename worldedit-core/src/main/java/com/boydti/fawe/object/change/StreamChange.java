package com.boydti.fawe.object.change;

import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

public interface StreamChange {
    public void flushChanges(FaweOutputStream out) throws IOException;

    public void undoChanges(FaweInputStream in) throws IOException;

    public void redoChanges(FaweInputStream in) throws IOException;

    default void flushChanges(File file) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            try (LZ4BlockOutputStream compressed = new LZ4BlockOutputStream(out)) {
//                compressed.setLevel(Deflater.BEST_SPEED);
                try (FaweOutputStream fos = new FaweOutputStream(compressed)) {
                    flushChanges(fos);
                }
            }
        }
    }

    default void undoChanges(File file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            try (LZ4BlockInputStream compressed = new LZ4BlockInputStream(in)) {
                try (FaweInputStream fis = new FaweInputStream(compressed)) {
                    undoChanges(fis);
                }
            }
        }
    }

    default void redoChanges(File file) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            try (LZ4BlockInputStream compressed = new LZ4BlockInputStream(in)) {
                try (FaweInputStream fis = new FaweInputStream(compressed)) {
                    redoChanges(fis);
                }
            }
        }
    }
}
