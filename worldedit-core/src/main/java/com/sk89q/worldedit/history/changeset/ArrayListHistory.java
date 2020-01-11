/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.history.changeset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.object.changeset.SimpleChangeSetSummary;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores all {@link Change}s in an {@link ArrayList}.
 */
public class ArrayListHistory implements ChangeSet {

    private final List<Change> changes = new ArrayList<>();

    private boolean recordChanges = true;

    @Override
    public void add(Change change) {
        checkNotNull(change);
        if (recordChanges) {
            changes.add(change);
        }
    }

    @Override
    public boolean isRecordingChanges() {
        return recordChanges;
    }

    @Override
    public void setRecordChanges(boolean recordChanges) {
        this.recordChanges = recordChanges;
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return Lists.reverse(changes).iterator();
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return changes.iterator();
    }

    @Override
    public int size() {
        return changes.size();
    }

    @Override
    public ChangeSetSummary summarize(Region region, boolean shallow) {
        SimpleChangeSetSummary summary = new SimpleChangeSetSummary();
        for (Change change : changes) {
            if (change instanceof BlockChange) {
                BlockChange blockChange = (BlockChange) change;
                BlockVector3 pos = blockChange.getPosition();
                summary.add(pos.getX(), pos.getZ(), blockChange.getCurrent().getOrdinal());
            }
        }
        return summary;
    }
}
