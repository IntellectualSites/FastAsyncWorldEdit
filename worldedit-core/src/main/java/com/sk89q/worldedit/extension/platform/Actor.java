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
import com.sk89q.worldedit.entity.Metadatable;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.auth.Subject;
import com.sk89q.worldedit.util.formatting.text.Component;

import java.io.File;
import java.text.NumberFormat;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.jetbrains.annotations.NotNull;

/**
 * An object that can perform actions in WorldEdit.
 */
public interface Actor extends Identifiable, SessionOwner, Subject, Metadatable {

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
     */
    void printRaw(String msg);

    /**
     * Print a WorldEdit message.
     *
     * @param msg The message text
     */
    void printDebug(String msg);

    /**
     * Print a WorldEdit message.
     *
     * @param msg The message text
     */
    void print(String msg);

    /**
     * Print a WorldEdit error.
     *
     * @param msg The error message text
     */
    void printError(String msg);

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

    default void checkConfirmationStack(@NotNull Runnable task, @NotNull String command,
        Region region, int times, InjectedValueAccess context) throws RegionOperationException {
        if (!getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                long area =
                    (long) ((max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1)) * times;
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY() * times;
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(min, max, command,
                            NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    default void checkConfirmationRegion(@NotNull Runnable task, @NotNull String command,
        Region region, InjectedValueAccess context) throws RegionOperationException {
        if (!getMeta("cmdConfirmRunning", false)) {
            if (region != null) {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                long area = (max.getX() - min.getX()) * (max.getZ() - min.getZ() + 1);
                if (area > 2 << 18) {
                    setConfirmTask(task, context, command);
                    BlockVector3 base = max.subtract(min).add(BlockVector3.ONE);
                    long volume = (long) base.getX() * base.getZ() * base.getY();
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(min, max, command,
                            NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    default void setConfirmTask(@NotNull Runnable task, InjectedValueAccess context,
        @NotNull String command) {
        CommandEvent event = new CommandEvent(this, command);
        Runnable newTask = () -> PlatformCommandManager.getInstance().handleCommandTask(() -> {
            task.run();
            return null;
        }, context, getSession(), event);
        setMeta("cmdConfirm", newTask);
    }

    default void checkConfirmation(@NotNull Runnable task, @NotNull String command, int times,
        int limit, InjectedValueAccess context) throws RegionOperationException {
        if (!getMeta("cmdConfirmRunning", false)) {
            if (times > limit) {
                setConfirmTask(task, context, command);
                String volume = "<unspecified>";
                throw new RegionOperationException(
                    BBC.WORLDEDIT_CANCEL_REASON_CONFIRM.format(0, times, command, volume));
            }
        }
        task.run();
    }

    default void checkConfirmationRadius(@NotNull Runnable task, String command, int radius,
        InjectedValueAccess context) throws RegionOperationException {
        if (command != null && !getMeta("cmdConfirmRunning", false)) {
            if (radius > 0) {
                if (radius > 448) {
                    setConfirmTask(task, context, command);
                    long volume = (long) (Math.PI * ((double) radius * radius));
                    throw new RegionOperationException(BBC.WORLDEDIT_CANCEL_REASON_CONFIRM
                        .format(0, radius, command,
                            NumberFormat.getNumberInstance().format(volume)));
                }
            }
        }
        task.run();
    }

    default boolean confirm() {
        Runnable confirm = deleteMeta("cmdConfirm");
        if (confirm == null) {
            return false;
        }
        queueAction(() -> {
            setMeta("cmdConfirmRunning", true);
            try {
                confirm.run();
            } finally {
                setMeta("cmdConfirmRunning", false);
            }
        });
        return true;
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
}
