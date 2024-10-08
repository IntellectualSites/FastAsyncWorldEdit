/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.history.changeset;

import com.fastasyncworldedit.core.history.changeset.ChangeSetSummary;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tracks a set of undoable operations and allows their undo and redo. The
 * entirety of a change set should be undone and redone at once.
 */
//FAWE start - extends Closable
public interface ChangeSet extends Closeable {
//FAWE end

    /**
     * Add the given change to the history.
     *
     * @param change the change
     */
    void add(Change change);

    /**
     * Whether or not the ChangeSet is recording changes.
     *
     * @return whether or not the ChangeSet is set to record changes
     */
    boolean isRecordingChanges();

    /**
     * Tell the change set whether to record changes or not.
     *
     * @param recordChanges whether to record changes or not
     */
    void setRecordChanges(boolean recordChanges);

    /**
     * Get a backward directed iterator that can be used for undo.
     *
     * <p>The iterator may return the changes out of order, as long as the final
     * result after all changes have been applied is correct.</p>
     *
     * @return a undo directed iterator
     */
    Iterator<Change> backwardIterator();

    /**
     * Get a forward directed iterator that can be used for redo.
     *
     * <p>The iterator may return the changes out of order, as long as the final
     * result after all changes have been applied is correct.</p>
     *
     * @return a forward directed iterator
     */
    Iterator<Change> forwardIterator();

    /**
     * Get the number of stored changes.
     *
     * @return the change count
     * @deprecated History could be larger than int max value so FAWE prefers {@link ChangeSet#longSize()}
     */
    @Deprecated(since = "2.11.1")
    int size();

    //FAWE start

    /**
     * Get the number of stored changes.
     * History could be larger than int max value so FAWE prefers this method.
     *
     * @return the change count
     * @since 2.11.1
     */
    default long longSize() {
        return size();
    }

    /**
     * Close the changeset.
     */
    @Override
    default void close() throws IOException {
    }

    /**
     * Delete the changeset (e.g. files on disk, or in a database)
     */
    default void delete() {
    }

    /**
     * Get a summary of this changeset.
     */
    @Nullable
    default ChangeSetSummary summarize(Region region, boolean shallow) {
        return null;
    }

    /**
     * Get if the changeset is empty (i.e. size == 0)
     *
     * @return is empty
     */
    default boolean isEmpty() {
        return size() == 0;
    }
    //FAWE end
}
