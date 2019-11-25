package com.boydti.fawe.object.task;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.TaskManager;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SingleThreadIntervalQueue<T> {
    private final ConcurrentMap<T, Long> objMap = new ConcurrentHashMap<>();
    private final Runnable task;
    private AtomicBoolean queued = new AtomicBoolean();

    public SingleThreadIntervalQueue(int interval) {
        this.task = new Runnable() {
            @Override
            public void run() {
                long allowedTick = Fawe.get().getTimer().getTick() - 1;
                Iterator<Map.Entry<T, Long>> iter = objMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<T, Long> entry = iter.next();
                    Long time = entry.getValue();
                    if (time < allowedTick) {
                        T obj = entry.getKey();
                        iter.remove();
                        operate(obj);
                    }
                }
                synchronized (objMap) {
                    if (!objMap.isEmpty()) TaskManager.IMP.laterAsync(this, interval);
                    else queued.set(false);
                }
            }
        };
    }

    public abstract void operate(T obj);

    public boolean dequeue(T obj) {
        synchronized (objMap) {
            return objMap.remove(obj) != null;
        }
    }

    public void queue(T obj) {
        synchronized (objMap) {
            objMap.put(obj, Fawe.get().getTimer().getTick());
            if (!queued.get()) {
                queued.set(true);
                TaskManager.IMP.laterAsync(task, 3);
            }
        }
    }
}
