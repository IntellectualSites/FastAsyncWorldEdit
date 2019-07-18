package com.boydti.fawe.object.change;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.HasIQueueExtent;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.io.File;
import java.io.IOException;


import static com.google.common.base.Preconditions.checkNotNull;

public class CFIChange implements Change {
    private final File file;

    public CFIChange(File file) {
        checkNotNull(file);
        this.file = file;
    }

    private HeightMapMCAGenerator getQueue(UndoContext context) {
        ExtentTraverser found = new ExtentTraverser(context.getExtent()).find(HasIQueueExtent.class);
        if (found != null) {
            IQueueExtent queue = ((HasIQueueExtent) found.get()).getQueue();
            if (queue instanceof HeightMapMCAGenerator) return (HeightMapMCAGenerator) queue;
        }
        Fawe.debug("FAWE does not support: " + context.getExtent() + " for " + getClass() + " (bug Empire92)");
        return null;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        HeightMapMCAGenerator queue = getQueue(context);
        if (queue != null) {
            try {
                queue.undoChanges(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            queue.update();
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        HeightMapMCAGenerator queue = getQueue(context);
        if (queue != null) {
            try {
                queue.redoChanges(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            queue.update();
        }
    }
}
