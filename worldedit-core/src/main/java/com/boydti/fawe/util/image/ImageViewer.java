package com.boydti.fawe.util.image;

import java.io.Closeable;

public interface ImageViewer extends Closeable{
    public void view(Drawable drawable);
}
