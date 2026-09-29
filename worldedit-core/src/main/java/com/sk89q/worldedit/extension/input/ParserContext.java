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

package com.sk89q.worldedit.extension.input;

import com.fastasyncworldedit.core.configuration.Caption;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.factory.MaskFactory;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Locatable;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.enginehub.piston.inject.InjectedValueAccess;

import javax.annotation.Nullable;

/**
 * Contains contextual information that may be useful when constructing
 * objects from a registry (such as {@link MaskFactory}).
 *
 * <p>By default, {@link #isRestricted()} will return true.</p>
 */
public class ParserContext {

    @Nullable
    private Extent extent;
    @Nullable
    private LocalSession session;
    @Nullable
    private World world;
    @Nullable
    private Actor actor;
    private boolean restricted = true;
    private boolean tryLegacy = true;
    private boolean preferringWildcard;
    //Fawe start
    private InjectedValueAccess injected;
    private int minY = Integer.MIN_VALUE;
    private int maxY = Integer.MAX_VALUE;
    //FAWE end

    /**
     * Create a new instance.
     */
    public ParserContext() {
    }

    /**
     * Creates a copy of another instance.
     *
     * @param other the other instance
     */
    public ParserContext(ParserContext other) {
        setExtent(other.getExtent());
        setSession(other.getSession());
        setWorld(other.getWorld());
        setActor(other.getActor());
        setRestricted(other.isRestricted());
        setPreferringWildcard(other.isPreferringWildcard());
        setTryLegacy(other.isTryingLegacy());
    }

    /**
     * Get the {@link Extent} set on this context.
     *
     * @return an extent
     */
    @Nullable
    public Extent getExtent() {
        return extent;
    }

    /**
     * Set the extent.
     *
     * @param extent an extent, or null if none is available
     */
    public void setExtent(@Nullable Extent extent) {
        this.extent = extent;
    }

    /**
     * Get the {@link LocalSession}.
     *
     * @return a session
     */
    @Nullable
    public LocalSession getSession() {
        return session;
    }

    /**
     * Set the session.
     *
     * @param session a session, or null if none is available
     */
    public void setSession(@Nullable LocalSession session) {
        this.session = session;
    }

    /**
     * Get the {@link World} set on this context.
     *
     * @return a world
     */
    @Nullable
    public World getWorld() {
        return world;
    }

    /**
     * Set the world.
     *
     * @param world a world, or null if none is available
     */
    public void setWorld(@Nullable World world) {
        this.world = world;
        //FAWE start - only set extent to world if null
        if (extent == null) {
            setExtent(world);
        }
        //FAWE end
    }

    /**
     * Get the {@link Actor} set on this context.
     *
     * @return an actor, or null
     */
    @Nullable
    public Actor getActor() {
        return actor;
    }

    /**
     * Set the actor.
     *
     * @param actor an actor, or null if none is available
     */
    public void setActor(@Nullable Actor actor) {
        this.actor = actor;
    }

    /**
     * Get the {@link Extent} set on this context.
     *
     * @return an extent
     * @throws InputParseException thrown if no {@link Extent} is set
     */
    public Extent requireExtent() throws InputParseException {
        Extent extent = getExtent();
        if (extent == null) {
            throw new InputParseException(Caption.of("worldedit.error.missing-extent"));
        }
        return extent;
    }

    /**
     * Get the {@link LocalSession}.
     *
     * @return a session
     * @throws InputParseException thrown if no {@link LocalSession} is set
     */
    public LocalSession requireSession() throws InputParseException {
        LocalSession session = getSession();
        if (session == null) {
            throw new InputParseException(Caption.of("worldedit.error.missing-session"));
        }
        return session;
    }

    /**
     * Get the {@link World} set on this context.
     *
     * @return a world
     * @throws InputParseException thrown if no {@link World} is set
     */
    public World requireWorld() throws InputParseException {
        World world = getWorld();
        if (world == null) {
            throw new InputParseException(Caption.of("worldedit.error.missing-world"));
        }
        return world;
    }

    /**
     * Get the {@link Actor} set on this context.
     *
     * @return an actor
     * @throws InputParseException thrown if no {@link Actor} is set
     */
    public Actor requireActor() throws InputParseException {
        Actor actor = getActor();
        if (actor == null) {
            throw new InputParseException(Caption.of("worldedit.error.missing-actor"));
        }
        return actor;
    }

    /**
     * Get the {@link Player} set on this context.
     *
     * @return a player
     * @throws InputParseException thrown if no {@link Actor} is set
     */
    public Player requirePlayer() throws InputParseException {
        Actor actor = getActor();
        if (!(actor instanceof Player player)) {
            throw new InputParseException(Caption.of("worldedit.error.missing-player"));
        }
        return player;
    }

    /**
     * Returns whether there should be restrictions (as a result of
     * limits or permissions) considered when parsing the input.
     *
     * @return true if restricted
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Set whether there should be restrictions (as a result of
     * limits or permissions) considered when parsing the input.
     *
     * @param restricted true if restricted
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    /**
     * Get whether wildcards are preferred.
     *
     * @return true if wildcards are preferred
     */
    public boolean isPreferringWildcard() {
        return preferringWildcard;
    }

    /**
     * Set whether wildcards are preferred.
     *
     * @param preferringWildcard true if wildcards are preferred
     */
    public void setPreferringWildcard(boolean preferringWildcard) {
        this.preferringWildcard = preferringWildcard;
    }

    /**
     * Set whether legacy IDs should be attempted.
     *
     * @param tryLegacy true if legacy IDs should be attempted
     */
    public void setTryLegacy(boolean tryLegacy) {
        this.tryLegacy = tryLegacy;
    }

    /**
     * Get whether legacy IDs should be tried.
     *
     * @return true if legacy should be tried
     */
    public boolean isTryingLegacy() {
        return tryLegacy;
    }

    //FAWE start
    public void setInjected(InjectedValueAccess injected) {
        this.injected = injected;
    }

    public InjectedValueAccess getInjected() {
        return injected;
    }

    /**
     * Attempts to resolve the minimum Y value associated with this context or returns 0.
     * Caches both min and max y values.
     *
     * @return Minimum y value (inclusive) or 0
     */
    public int getMinY() {
        if (minY != Integer.MIN_VALUE) {
            return minY;
        }

        Extent extent = null;

        if (actor instanceof Locatable) {
            extent = ((Locatable) actor).getExtent();
        } else if (world != null) {
            extent = world;
        } else if (this.extent != null) {
            extent = this.extent;
        }

        if (extent != null) {
            minY = extent.getMinY();
            maxY = extent.getMaxY();
        } else {
            minY = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMinY();
            maxY = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING).versionMaxY();
        }

        return minY;
    }

    /**
     * Attempts to resolve the maximum Y value associated with this context or returns 255.
     * Caches both min and max y values.
     *
     * @return Maximum y value (inclusive) or 255
     */
    public int getMaxY() {
        if (maxY != Integer.MAX_VALUE) {
            return maxY;
        }

        Extent extent = null;

        if (actor instanceof Locatable) {
            extent = ((Locatable) actor).getExtent();
        } else if (world != null) {
            extent = world;
        } else if (this.extent != null) {
            extent = this.extent;
        }

        if (extent != null) {
            minY = extent.getMinY();
            maxY = extent.getMaxY();
        } else {
            minY = 0;
            maxY = 255;
        }

        return maxY;
    }

    /**
     * Attempts to retrieve the selection associated with this context. Requires an {@link Actor} or {@link LocalSession} be
     * supplied.
     *
     * @return Region representing the selection for this context or null if it cannot be retrieved.
     * @since 2.2.0
     */
    public Region getSelection() {
        if (session != null) {
            try {
                return session.getSelection();
            } catch (IncompleteRegionException ignored) {
            }
        }
        if (actor != null) {
            try {
                return actor.getSession().getSelection();
            } catch (IncompleteRegionException ignored) {
            }
        }
        return null;
    }

    //FAWE end
}
