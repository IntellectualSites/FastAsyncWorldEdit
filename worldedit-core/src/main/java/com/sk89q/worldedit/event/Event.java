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

package com.sk89q.worldedit.event;

import com.sk89q.worldedit.WorldEdit;

/**
 * An abstract base class for all WorldEdit events.
 */
public abstract class Event {
    //FAWE start

    /**
     * Returns true if this event was called and not cancelled.
     *
     * @return !isCancelled
     */
    public boolean call() {
        WorldEdit.getInstance().getEventBus().post(this);
        if (this instanceof Cancellable) {
            return !((Cancellable) this).isCancelled();
        }
        return true;
    }
    //FAWE end
}
