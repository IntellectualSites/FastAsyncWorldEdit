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

package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import java.util.Iterator;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visits entities as provided by an {@code Iterator}.
 */
public class EntityVisitor implements Operation {

    private final EntityFunction function;
    private int affected = 0;
    private final Iterator<? extends Entity> iterator;

    /**
     * Create a new instance.
     *
     * @param iterator the iterator
     * @param function the function
     */
    public EntityVisitor(final Iterator<? extends Entity> iterator, final EntityFunction function) {
        checkNotNull(iterator);
        checkNotNull(function);

        this.function = function;
        this.iterator = iterator;
    }

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return this.affected;
    }

    @Override
    public Operation resume(final RunContext run) throws WorldEditException {
        while (this.iterator.hasNext()) {
            if (this.function.apply(this.iterator.next())) {
                affected++;
            }
        }
        return null;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addStatusMessages(final List<String> messages) {
        messages.add(BBC.VISITOR_ENTITY.format(getAffected()));
    }


}
