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

package com.sk89q.worldedit.extension.platform;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.brush.visualization.VirtualWorld;
import com.boydti.fawe.util.task.InterruptableCondition;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.MapMetadatable;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.auth.Subject;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.concurrent.locks.Condition;
import java.util.Locale;

/**
 * An object that can perform actions in WorldEdit.
 */
public interface Actor extends Identifiable, SessionOwner, Subject, MapMetadatable {

    /**
     * Get the name of the actor.
     *
     * @return String
     */
    String getName();

    /**
     * Gets the display name of the actor. This can be a nickname, and is not guaranteed to be unique.
     *
     * @return The display name
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * Print a message.
     *
     * @param msg The message text
     * @deprecated Use component-based functions (print)
     */
    @Deprecated
    void printRaw(String msg);

    /**
     * Print a WorldEdit message.
     *
     * @param msg The message text
     * @deprecated Use component-based functions (printDebug)
     */
    @Deprecated
    void printDebug(String msg);

    /**
     * Print a WorldEdit message.
     *
     * @param msg The message text
     * @deprecated Use component-based functions (printInfo)
     */
    @Deprecated
    void print(String msg);

    /**
     * Print a WorldEdit error.
     *
     * @param msg The error message text
     * @deprecated Use component-based functions (printError)
     */
    @Deprecated
    void printError(String msg);

    /**
     * Print a WorldEdit error.
     *
     * @param component The component to print
     */
    default void printError(Component component) {
        print(component.color(TextColor.RED));
    }

    /**
     * Print a WorldEdit message.
     *
     * @param component The component to print
     */
    default void printInfo(Component component) {
        print(component.color(TextColor.LIGHT_PURPLE));
    }

    /**
     * Print a {@link Component}.
     *
     * @param component The component to print
     */
    void print(Component component);

    /**
     * Returns true if the actor can destroy bedrock.
     *
     * @return true if bedrock can be broken by the actor
     */
    boolean canDestroyBedrock();

    /**
     * Print a WorldEdit message.
     *
     * @param component The component to print
     */
    default void printDebug(Component component) {
        print(component.color(TextColor.GRAY));
    }

    /**
     * Return whether this actor is a player.
     *
     * @return true if a player
     */
    boolean isPlayer();

    /**
     * Open a file open dialog.
     *
     * @param extensions null to allow all
     * @return the selected file or null if something went wrong
     */
    File openFileOpenDialog(String[] extensions);

    /**
     * Open a file save dialog.
     *
     * @param extensions null to allow all
     * @return the selected file or null if something went wrong
     */
    File openFileSaveDialog(String[] extensions);

    /**
     * Send a CUI event.
     *
     * @param event the event
     */
    void dispatchCUIEvent(CUIEvent event);

    boolean runAction(Runnable ifFree, boolean checkFree, boolean async);

    /**
     * Decline any pending actions
     * @return true if an action was pending
     */
    default boolean decline() {
        InterruptableCondition confirm = deleteMeta("cmdConfirm");
        if (confirm != null) {
            confirm.interrupt();
            return true;
        }
        return false;
    }

    /**
     * Confirm any pending actions
     * @return true if an action was pending
     */
    default boolean confirm() {
        InterruptableCondition confirm = deleteMeta("cmdConfirm");
        if (confirm != null) {
            confirm.signal();;
            return true;
        }
        return false;
    }

    /**
     * Queue an action to run async
     *
     * @param run
     */
    default void queueAction(Runnable run) {
        runAction(run, false, true);
    }

    default boolean checkAction() {
        long time = getMeta("faweActionTick", Long.MIN_VALUE);
        long tick = Fawe.get().getTimer().getTick();
        setMeta("faweActionTick", tick);
        return tick > time;
    }

    default FaweLimit getLimit() {
        return Settings.IMP.getLimit(this);
    }

    default boolean runAsyncIfFree(Runnable r) {
        return runAction(r, true, true);
    }

    default boolean runIfFree(Runnable r) {
        return runAction(r, true, false);
    }

    /**
     * Attempt to cancel all pending and running actions
     * @param close if Extents are closed
     * @return number of cancelled actions
     */
    default int cancel(boolean close) {
        int cancelled = decline() ? 1 : 0;

        for (Request request : Request.getAll()) {
            EditSession editSession = request.getEditSession();
            if (editSession != null) {
                Player player = editSession.getPlayer();
                if (equals(player)) {
                    editSession.cancel();
                    cancelled++;
                }
            }
        }
        VirtualWorld world = getSession().getVirtualWorld();
        if (world != null) {
            if (close) {
                try {
                    world.close(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    world.close(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cancelled;
    }

    /**
     * Get the locale of this actor.
     *
     * @return The locale
     */
    Locale getLocale();
}
