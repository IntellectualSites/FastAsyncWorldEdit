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

import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.task.SimpleAsyncNotifyQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.internal.cui.CUIEvent;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractNonPlayerActor implements Actor {

    @Override
    public boolean canDestroyBedrock() {
        return true;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public File openFileOpenDialog(String[] extensions) {
        return null;
    }

    @Override
    public File openFileSaveDialog(String[] extensions) {
        return null;
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
    }

    private final ConcurrentHashMap<String, Object> meta = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getRawMeta() {
        return meta;
    }

    // Queue for async tasks
    private AtomicInteger runningCount = new AtomicInteger();
    private SimpleAsyncNotifyQueue asyncNotifyQueue = new SimpleAsyncNotifyQueue(
    (thread, throwable) -> {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        if (throwable instanceof WorldEditException) {
            printError(throwable.getLocalizedMessage());
        } else {
            FaweException fe = FaweException.get(throwable);
            if (fe != null) {
                printError(fe.getComponent());
            } else {
                throwable.printStackTrace();
            }
        }
    });

    /**
     * Run a task either async, or on the current thread
     *
     * @param ifFree
     * @param checkFree Whether to first check if a task is running
     * @param async
     * @return false if the task was ran or queued
     */
    @Override
    public boolean runAction(Runnable ifFree, boolean checkFree, boolean async) {
        if (checkFree) {
            if (runningCount.get() != 0) {
                return false;
            }
        }
        Runnable wrapped = () -> {
            try {
                runningCount.addAndGet(1);
                ifFree.run();
            } finally {
                runningCount.decrementAndGet();
            }
        };
        if (async) {
            asyncNotifyQueue.queue(wrapped);
        } else {
            TaskManager.IMP.taskNow(wrapped, false);
        }
        return true;
    }
}
