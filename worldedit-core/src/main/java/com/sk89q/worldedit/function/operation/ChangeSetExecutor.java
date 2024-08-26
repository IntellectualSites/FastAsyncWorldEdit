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

package com.sk89q.worldedit.function.operation;

import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.history.changeset.ChangeExchangeCoordinator;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Performs an undo or redo from a given {@link ChangeSet}.
 */
public class ChangeSetExecutor implements Operation {

    //FAWE start - Override
    public enum Type {
        UNDO {
            @Override
            public void perform(Change change, UndoContext context) {
                change.undo(context);
            }
        },
        REDO {
            @Override
            public void perform(Change change, UndoContext context) {
                change.redo(context);
            }
        };

        public void perform(Change change, UndoContext context) {
        }
    }
    //FAWE end

    private final Iterator<Change> iterator;
    private final ChangeExchangeCoordinator changeExchangeCoordinator;
    private final Type type;
    private final UndoContext context;

    /**
     * Create a new instance.
     *
     * @param changeSet the change set
     * @param type      type of change
     * @param context   the undo context
     */
    //FAWE start - BlockBag & inventory
    private ChangeSetExecutor(ChangeSet changeSet, Type type, UndoContext context, BlockBag blockBag, int inventory) {
        checkNotNull(changeSet);
        checkNotNull(type);
        checkNotNull(context);

        this.type = type;
        this.context = context;
        if (changeSet instanceof AbstractChangeSet abstractChangeSet) {
            if (Settings.settings().EXPERIMENTAL.UNDO_BATCH_SIZE > 0) {
                this.changeExchangeCoordinator = abstractChangeSet.getCoordinatedChanges(blockBag, inventory, type == Type.REDO);
                this.iterator = null;
            } else {
                this.iterator = abstractChangeSet.getIterator(blockBag, inventory, type == Type.REDO);
                this.changeExchangeCoordinator = null;
            }
        } else if (type == Type.UNDO) {
            iterator = changeSet.backwardIterator();
            this.changeExchangeCoordinator = null;
        } else {
            iterator = changeSet.forwardIterator();
            this.changeExchangeCoordinator = null;
        }
    }
    //FAWE end

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        // FAWE start - ChangeExchangeCoordinator
        if (this.changeExchangeCoordinator != null) {
            try (this.changeExchangeCoordinator) {
                Change[] changes = new Change[Settings.settings().EXPERIMENTAL.UNDO_BATCH_SIZE];
                while ((changes = this.changeExchangeCoordinator.take(changes)) != null) {
                    for (final Change change : changes) {
                        if (change == null) {
                            return null; // end
                        }
                        type.perform(change, context);
                    }
                }
                return null;
            }
        }
        // FAWE end
        while (iterator.hasNext()) {
            Change change = iterator.next();
            //FAWE start - types > individual history step
            type.perform(change, context);
            //FAWE end
        }
        return null;
    }

    @Override
    public void cancel() {
    }

    //FAWE start
    public static ChangeSetExecutor create(
            ChangeSet changeSet,
            UndoContext context,
            Type type,
            BlockBag blockBag,
            int inventory
    ) {
        return new ChangeSetExecutor(changeSet, type, context, blockBag, inventory);
    }
    //FAWE end

    /**
     * Create a new undo operation.
     *
     * @param changeSet the change set
     * @param context   an undo context
     * @return an operation
     */
    public static ChangeSetExecutor createUndo(ChangeSet changeSet, UndoContext context) {
        return new ChangeSetExecutor(changeSet, Type.UNDO, context, null, 0);
    }

    /**
     * Create a new redo operation.
     *
     * @param changeSet the change set
     * @param context   an undo context
     * @return an operation
     */
    public static ChangeSetExecutor createRedo(ChangeSet changeSet, UndoContext context) {
        return new ChangeSetExecutor(changeSet, Type.REDO, context, null, 0);
    }

}
