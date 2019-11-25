package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.io.FastByteArrayOutputStream;

import java.util.function.BiFunction;

public class LinearClipboardBuilder {
    FastByteArrayOutputStream blocksOut = new FastByteArrayOutputStream();
    FastByteArrayOutputStream biomesOut = new FastByteArrayOutputStream();

    public int width, height, length;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public LinearClipboard build() {
        return null;
    }
}
