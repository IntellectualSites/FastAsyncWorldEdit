package com.fastasyncworldedit.core.util.progress;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.function.BiConsumer;

/**
 * The default progress tracker uses titles
 */
public class DefaultProgressTracker implements BiConsumer<DefaultProgressTracker.ProgressType, Integer> {

    private final Player player;
    private final long start;
    private int delay = Settings.settings().QUEUE.PROGRESS.DELAY;
    private int interval = Settings.settings().QUEUE.PROGRESS.INTERVAL;

    public DefaultProgressTracker(Player player) {
        this.start = System.currentTimeMillis();
        this.player = player;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getInterval() {
        return interval;
    }

    public int getDelay() {
        return delay;
    }

    public Player getPlayer() {
        return player;
    }

    // Number of times a chunk was queued
    private int totalQueue = 0;
    // Current size of the queue
    private int amountQueue = 0;
    // Number of chunks dispatched
    private int amountDispatch = 0;
    // Last size (to calculate speed)
    private final int lastSize = 0;
    // If the task is finished
    private boolean done = false;

    public enum ProgressType {
        DISPATCH,
        QUEUE,
        DONE
    }

    @Override
    public void accept(ProgressType type, Integer amount) {
        switch (type) {
            case DISPATCH -> {
                amountDispatch++;
                amountQueue = amount;
            }
            case QUEUE -> {
                totalQueue++;
                amountQueue = amount;
            }
            case DONE -> {
                if (totalQueue > 64 && !done) {
                    done = true;
                    done();
                }
                return;
            }
        }
        // Only send a message after 64 chunks (i.e. ignore smaller edits)
        long now = System.currentTimeMillis();
        if (now - start > delay) {
            long currentTick = now / 50;
            if (currentTick > lastTick + interval) {
                lastTick = currentTick;
                send();
            }
        }
    }

    private void done() {
        // TODO (folia)
        // TaskManager.taskManager().task(this::doneTask);
    }

    private long lastTick = 0;

    private void send() {
        // Run on main thread
        // TODO (folia)
        // TaskManager.taskManager().task(this::sendTask);
    }

    public void doneTask() {
        sendTile(TextComponent.empty(), Caption.of("fawe.progress.progress.finished"));
    }

    public void sendTask() {
        String queue = StringMan.padRight(String.valueOf(totalQueue), 3);
        String dispatch = StringMan.padLeft(String.valueOf(amountDispatch), 3);
        int total = amountDispatch != 0 ? amountDispatch : amountQueue;
        int speed = total != 0 ? (int) (total / Math.max((System.currentTimeMillis() - start) / 1000d, 1)) : 0;
        String speedStr = StringMan.padRight(String.valueOf(speed), 3);
        String percent = StringMan.padRight(
                String.valueOf(amountDispatch != 0 ? (amountDispatch * 100) / totalQueue : 0), 3);
        int remaining = speed != 0 ? amountQueue / speed : -1;
        sendTile(
                TextComponent.empty(),
                Caption.of(
                        "fawe.progress.progress.message",
                        queue,
                        dispatch,
                        percent,
                        StringMan.padLeft("" + speed, 3),
                        StringMan.padLeft("" + remaining, 3)
                )
        );
    }

    public void sendTile(Component title, Component sub) {
        player.sendTitle(title, sub);
    }

}
