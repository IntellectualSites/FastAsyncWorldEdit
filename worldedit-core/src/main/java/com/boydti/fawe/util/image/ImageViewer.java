package com.boydti.fawe.util.image;

import java.io.Closeable;

public interface ImageViewer extends Closeable{
    void view(Drawable drawable);
}
