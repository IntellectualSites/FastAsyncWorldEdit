package com.fastasyncworldedit.bukkit.util.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * Unified scheduler interface that works across both Bukkit and Folia servers.
 * This interface provides a consistent API for scheduling tasks regardless of the server type.
 */
public interface Scheduler {

    /**
     * Schedules a repeating task that runs on the main thread.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The initial delay in ticks
     * @param period The period between executions in ticks
     */
    void runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period);

    /**
     * Schedules a task to run on the main thread.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     */
    void runTask(Plugin plugin, Runnable runnable);

    /**
     * Schedules a task to run asynchronously.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     */
    void runTaskAsynchronously(Plugin plugin, Runnable runnable);

    /**
     * Schedules a delayed task to run on the main thread.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    void runTaskLater(Plugin plugin, Runnable runnable, long delay);

    /**
     * Schedules a delayed task to run asynchronously.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    void runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay);

    /**
     * Schedules a repeating task that runs on the main thread (uses library plugin).
     *
     * @param runnable The task to run
     * @param delay The initial delay in ticks
     * @param period The period between executions in ticks
     */
    void runTaskTimer(Runnable runnable, long delay, long period);

    /**
     * Schedules a task to run on the main thread (uses library plugin).
     *
     * @param runnable The task to run
     */
    void runTask(Runnable runnable);

    /**
     * Schedules a task to run asynchronously (uses library plugin).
     *
     * @param runnable The task to run
     */
    void runTaskAsynchronously(Runnable runnable);

    /**
     * Schedules a delayed task to run on the main thread (uses library plugin).
     *
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    void runTaskLater(Runnable runnable, long delay);

    /**
     * Schedules a delayed task to run asynchronously (uses library plugin).
     *
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    void runTaskLaterAsynchronously(Runnable runnable, long delay);

    /**
     * Legacy method for scheduling a delayed sync task.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The delay in ticks
     */
    void scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay);

    /**
     * Runs a task and returns a cancellable task instance.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @return Cancellable task instance
     */
    CancellableTask runTaskCancellable(Plugin plugin, Runnable runnable);

    /**
     * Runs a delayed task and returns a cancellable task instance.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The delay in ticks
     * @return Cancellable task instance
     */
    CancellableTask runTaskLaterCancellable(Plugin plugin, Runnable runnable, long delay);

    /**
     * Runs a repeating task and returns a cancellable task instance.
     *
     * @param plugin The plugin instance
     * @param runnable The task to run
     * @param delay The initial delay in ticks
     * @param period The period between executions in ticks
     * @return Cancellable task instance
     */
    CancellableTask runTaskTimerCancellable(Plugin plugin, Runnable runnable, long delay, long period);

    /**
     * Runs a task and returns a cancellable task instance (uses library plugin).
     *
     * @param runnable The task to run
     * @return Cancellable task instance
     */
    CancellableTask runTaskCancellable(Runnable runnable);

    /**
     * Runs a delayed task and returns a cancellable task instance (uses library plugin).
     *
     * @param runnable The task to run
     * @param delay The delay in ticks
     * @return Cancellable task instance
     */
    CancellableTask runTaskLaterCancellable(Runnable runnable, long delay);

    /**
     * Runs a repeating task and returns a cancellable task instance (uses library plugin).
     *
     * @param runnable The task to run
     * @param delay The initial delay in ticks
     * @param period The period between executions in ticks
     * @return Cancellable task instance
     */
    CancellableTask runTaskTimerCancellable(Runnable runnable, long delay, long period);

    /**
     * Interface for cancellable tasks that work across both Bukkit and Folia.
     */
    interface CancellableTask {

        /**
         * Cancels this task.
         */
        void cancel();

        /**
         * Checks if this task is cancelled.
         * @return true if cancelled, false otherwise
         */
        boolean isCancelled();
    }
}
