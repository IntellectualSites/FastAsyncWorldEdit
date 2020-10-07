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

package com.sk89q.worldedit.session.request;

import com.boydti.fawe.object.collection.CleanableThreadLocal;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Describes the current request using a {@link ThreadLocal}.
 */
public final class Request {

    private static final CleanableThreadLocal<Request> threadLocal = new CleanableThreadLocal<>(Request::new);

    @Nullable
    private World world;
    @Nullable
    private Actor actor;
    @Nullable
    private LocalSession session;
    @Nullable
    private EditSession editSession;
    @Nullable
    private Extent extent;
    private boolean valid;

    private Request() {
    }

    public static List<Request> getAll() {
        return threadLocal.getAll();
    }

    /**
     * Get the request world.
     *
     * @return the world, which may be null
     */
    @Nullable
    public World getWorld() {
        return world;
    }

    /**
     * Set the request world.
     *
     * @param world the world, which may be null
     */
    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    public void setExtent(@Nullable Extent extent) {
        this.extent = extent;
    }

    @Nullable
    public Extent getExtent() {
        if (extent != null) {
            return extent;
        }
        if (editSession != null) {
            return editSession;
        }
        if (world != null) {
            return world;
        }
        return null;
    }

    @Nullable
    public Actor getActor() {
        return actor;
    }

    public void setActor(@Nullable Actor actor) {
        this.actor = actor;
    }

    /**
     * Get the request session.
     *
     * @return the session, which may be null
     */
    @Nullable
    public LocalSession getSession() {
        return session;
    }

    /**
     * Get the request session.
     *
     * @param session the session, which may be null
     */
    public void setSession(@Nullable LocalSession session) {
        this.session = session;
    }

    /**
     * Get the {@link EditSession}.
     *
     * @return the edit session, which may be null
     */
    @Nullable
    public EditSession getEditSession() {
        return editSession;
    }

    /**
     * Set the {@link EditSession}.
     *
     * @param editSession the edit session, which may be null
     */
    public void setEditSession(@Nullable EditSession editSession) {
        this.editSession = editSession;
    }

    /**
     * Get the current request, which is specific to the current thread.
     *
     * @return the current request
     */
    public static Request request() {
        return threadLocal.get();
    }

    /**
     * Reset the current request and clear all fields.
     */
    public static void reset() {
        request().invalidate();
        threadLocal.remove();
    }

    /**
     * Check if the current request object is still valid. Invalid requests may contain outdated values.
     *
     * @return true if the request is valid
     */
    public boolean isValid() {
        return valid;
    }

    private void invalidate() {
        valid = false;
    }
}
