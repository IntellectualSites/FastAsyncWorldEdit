package com.boydti.fawe.object.change;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.object.brush.visualization.cfi.HeightMapMCAGenerator;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import java.io.File;
import java.io.IOException;

public class CFIChange implements Change {
    private final File file;

    public CFIChange(File file) {
        checkNotNull(file);
        this.file = file;
    }

    private HeightMapMCAGenerator getQueue(UndoContext context) {
        ExtentTraverser<HeightMapMCAGenerator> found = new ExtentTraverser<>(context.getExtent()).find(HeightMapMCAGenerator.class);
        if (found != null) {
            return found.get();
        }
        getLogger(CFIChange.class).debug("FAWE does not support: " + context.getExtent() + " for " + getClass() + " (bug Empire92)");
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
