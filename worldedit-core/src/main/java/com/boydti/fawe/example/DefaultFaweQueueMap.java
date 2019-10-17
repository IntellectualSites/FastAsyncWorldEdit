package com.boydti.fawe.example;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.SetQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultFaweQueueMap implements IFaweQueueMap {

    private final MappedFaweQueue parent;

    public DefaultFaweQueueMap(MappedFaweQueue parent) {
        this.parent = parent;
    }

    public final Long2ObjectOpenHashMap<FaweChunk> blocks = new Long2ObjectOpenHashMap<FaweChunk>() {
        @Override
        public FaweChunk put(Long key, FaweChunk value) {
            return put((long) key, value);
        }

        @Override
        public FaweChunk put(long key, FaweChunk value) {
            if (parent.getProgressTask() != null) {
                try {
                    parent.getProgressTask().run(FaweQueue.ProgressType.QUEUE, size() + 1);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            synchronized (this) {
                return super.put(key, value);
            }
        }
    };

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        synchronized (blocks) {
            return new HashSet<>(blocks.values());
        }
    }

    @Override
    public void forEachChunk(RunnableVal<FaweChunk> onEach) {
        synchronized (blocks) {
            for (Map.Entry<Long, FaweChunk> entry : blocks.entrySet()) {
                onEach.run(entry.getValue());
            }
        }
    }

    @Override
    public FaweChunk getFaweChunk(int cx, int cz) {
        if (cx == lastX && cz == lastZ) {
            return lastWrappedChunk;
        }
        long pair = MathMan.pairInt(cx, cz);
        FaweChunk chunk = this.blocks.get(pair);
        if (chunk == null) {
            chunk = this.getNewFaweChunk(cx, cz);
            FaweChunk previous = this.blocks.put(pair, chunk);
            if (previous != null) {
                blocks.put(pair, previous);
                return previous;
            }
            this.blocks.put(pair, chunk);
        }
        return chunk;
    }

    @Override
    public FaweChunk getCachedFaweChunk(int cx, int cz) {
        if (cx == lastX && cz == lastZ) {
            return lastWrappedChunk;
        }
        long pair = MathMan.pairInt(cx, cz);
        FaweChunk chunk = this.blocks.get(pair);
        lastWrappedChunk = chunk;
        return chunk;
    }

    @Override
    public void add(FaweChunk chunk) {
        long pair = MathMan.pairInt(chunk.getX(), chunk.getZ());
        FaweChunk previous = this.blocks.put(pair, chunk);
        if (previous != null) {
            blocks.put(pair, previous);
        }
    }


    @Override
    public void clear() {
        blocks.clear();
    }

    @Override
    public int size() {
        return blocks.size();
    }

    private FaweChunk getNewFaweChunk(int cx, int cz) {
        return parent.getFaweChunk(cx, cz);
    }

    private volatile FaweChunk lastWrappedChunk;
    private int lastX = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;

    @Override
    public boolean next(int amount, long time) {
        synchronized (blocks) {
            try {
                boolean skip = parent.getStage() == SetQueue.QueueStage.INACTIVE;
                int added = 0;
                Iterator<Map.Entry<Long, FaweChunk>> iter = blocks.entrySet().iterator();
                if (amount == 1) {
                    long start = System.currentTimeMillis();
                    do {
                        if (iter.hasNext()) {
                            FaweChunk chunk = iter.next().getValue();
                            if (skip && chunk == lastWrappedChunk) {
                                continue;
                            }
                            iter.remove();
                            parent.start(chunk);
                            chunk.call();
                            parent.end(chunk);
                        } else {
                            break;
                        }
                    } while (System.currentTimeMillis() - start < time);
                } else {
                    ExecutorCompletionService service = SetQueue.IMP.getCompleterService();
                    ForkJoinPool pool = SetQueue.IMP.getForkJoinPool();
                    boolean result = true;
                    // amount = 8;
                    for (int i = 0; i < amount && (result = iter.hasNext()); i++) {
                        Map.Entry<Long, FaweChunk> item = iter.next();
                        FaweChunk chunk = item.getValue();
                        if (skip && chunk == lastWrappedChunk) {
                            i--;
                            continue;
                        }
                        iter.remove();
                        parent.start(chunk);
                        service.submit(chunk);
                        added++;
                    }
                    // if result, then submitted = amount
                    if (result) {
                        long start = System.currentTimeMillis();
                        while (System.currentTimeMillis() - start < time && result) {
                            if (result = iter.hasNext()) {
                                Map.Entry<Long, FaweChunk> item = iter.next();
                                FaweChunk chunk = item.getValue();
                                if (skip && chunk == lastWrappedChunk) {
                                    continue;
                                }
                                iter.remove();
                                parent.start(chunk);
                                service.submit(chunk);
                                Future future = service.poll(50, TimeUnit.MILLISECONDS);
                                if (future != null) {
                                    FaweChunk fc = (FaweChunk) future.get();
                                    parent.end(fc);
                                }
                            }
                        }
                    }
                    pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    Future future;
                    while ((future = service.poll()) != null) {
                        FaweChunk fc = (FaweChunk) future.get();
                        parent.end(fc);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return !blocks.isEmpty();
        }
    }
}
