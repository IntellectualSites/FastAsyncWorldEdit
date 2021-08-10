package com.fastasyncworldedit.core.util.image;

import java.io.Closeable;

public interface ImageViewer extends Closeable {

    void view(Drawable drawable);

}
