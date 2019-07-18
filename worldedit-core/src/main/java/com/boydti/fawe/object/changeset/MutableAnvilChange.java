package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.HasIQueueExtent;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.ExtentTraverser;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MutableAnvilChange implements Change {
    private Path source;
    private Path destDir;

    public void setSource(Path source) {
        this.source = source;
    }

    private IQueueExtent queue;
    private boolean checkedQueue;

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        if (queue != null) {
            perform(queue);
        }
        if (!checkedQueue) {
            checkedQueue = true;
            Extent extent = context.getExtent();
            ExtentTraverser found = new ExtentTraverser(extent).find(HasIQueueExtent.class);
            if (found != null) {
                queue = ((HasIQueueExtent) found.get()).getQueue();
                destDir = queue.getSaveFolder().toPath();
                perform(queue);
            } else {
                Fawe.debug("FAWE does not support: " + extent + " for " + getClass() + " (bug Empire92)");
            }
        }
    }

    public void perform(IQueueExtent queue) {
        Path dest = destDir.resolve(source.getFileName());
        try {
            Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignore) {
            int[] coords = MainUtil.regionNameToCoords(source.toString());
            queue.setMCA(coords[0], coords[1], RegionWrapper.GLOBAL(), new Runnable() {
                @Override
                public void run() {
                    try {
                        Files.move(source, dest, StandardCopyOption.ATOMIC_MOVE);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }, false, true);
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        throw new UnsupportedOperationException("Redo not supported");
    }
}
