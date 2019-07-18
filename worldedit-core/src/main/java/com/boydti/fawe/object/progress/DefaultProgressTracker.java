package com.boydti.fawe.object.progress;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.beta.IQueueExtent;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;

/**
 * The default progress tracker uses titles
 */
public class DefaultProgressTracker extends RunnableVal2<IQueueExtent.ProgressType, Integer> {

    private final FawePlayer player;
    private final long start;
    private int delay = Settings.IMP.QUEUE.PROGRESS.DELAY;
    private int interval = Settings.IMP.QUEUE.PROGRESS.INTERVAL;

    public DefaultProgressTracker(FawePlayer player) {
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

    public FawePlayer getPlayer() {
        return player;
    }

    // Number of times a chunk was queued
    private int totalQueue = 0;
    // Current size of the queue
    private int amountQueue = 0;
    // Number of chunks dispatched
    private int amountDispatch = 0;
    // Last size (to calculate speed)
    private int lastSize = 0;
    // If the task is finished
    private boolean done = false;

    @Override
    public void run(IQueueExtent.ProgressType type, Integer amount) {
        switch (type) {
            case DISPATCH:
                amountDispatch++;
                amountQueue = amount;
                break;
            case QUEUE:
                totalQueue++;
                amountQueue = amount;
                break;
            case DONE:
                if (totalQueue > 64 && !done) {
                    done = true;
                    done();
                }
                return;
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

    private final void done() {
        TaskManager.IMP.task(this::doneTask);
    }

    private long lastTick = 0;

    private final void send() {
        // Run on main thread
        TaskManager.IMP.task(this::sendTask);
    }

    public void doneTask() {
        sendTile("", BBC.PROGRESS_FINISHED.s());
    }

    public void sendTask() {
        String queue = StringMan.padRight("" + totalQueue, 3);
        String dispatch = StringMan.padLeft("" + amountDispatch, 3);
        int total = amountDispatch != 0 ? amountDispatch : amountQueue;
        int speed = total != 0 ? (int) (total / Math.max((System.currentTimeMillis() - start) / 1000d, 1)) : 0;
        String speedStr = StringMan.padRight("" + speed, 3);
        String percent = StringMan.padRight("" + (amountDispatch != 0 ? (amountDispatch * 100) / totalQueue : 0), 3);
        int remaining = speed != 0 ? amountQueue / speed : -1;
        sendTile("", BBC.PROGRESS_MESSAGE.format(queue, dispatch, percent, StringMan.padLeft("" + speed, 3), StringMan.padLeft("" + remaining, 3)));
    }

    public void sendTile(String title, String sub) {
        player.sendTitle(title, sub);
    }
}
