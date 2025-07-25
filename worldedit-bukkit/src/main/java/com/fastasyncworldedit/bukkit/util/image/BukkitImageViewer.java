package com.fastasyncworldedit.bukkit.util.image;

import com.fastasyncworldedit.core.util.image.Drawable;
import com.fastasyncworldedit.core.util.image.ImageViewer;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.io.IOException;

/**
 * @deprecated for removal with no replacement. Out of scope for FAWE.
 */
@Deprecated(forRemoval = true, since = "TODO")
public class BukkitImageViewer implements ImageViewer {

    public BukkitImageViewer(Player player) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    public void selectFrame(ItemFrame start) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    public ItemFrame[][] getItemFrames() {
        throw new UnsupportedOperationException("No longer supported.");
    }

    @Override
    public void view(Drawable drawable) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    public void refresh() {
        throw new UnsupportedOperationException("No longer supported.");
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("No longer supported.");
    }

}
