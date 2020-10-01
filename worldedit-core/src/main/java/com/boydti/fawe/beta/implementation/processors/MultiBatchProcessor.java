package com.boydti.fawe.beta.implementation.processors;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.beta.Filter;
import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.util.StringMan;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.extent.Extent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.function.Supplier;

public class MultiBatchProcessor implements IBatchProcessor {
    private IBatchProcessor[] processors;
    private final LoadingCache<Class<?>, Map<Long, Filter>> classToThreadIdToFilter =
        FaweCache.IMP.createCache((Supplier<Map<Long, Filter>>) HashMap::new);

    public MultiBatchProcessor(IBatchProcessor... processors) {
        this.processors = processors;
    }

    public static IBatchProcessor of(IBatchProcessor... processors) {
        ArrayList<IBatchProcessor> list = new ArrayList<>();
        for (IBatchProcessor processor : processors) {
            if (processor instanceof MultiBatchProcessor) {
                list.addAll(Arrays.asList(((MultiBatchProcessor) processor).processors));
            } else if (!(processor instanceof EmptyBatchProcessor)) {
                list.add(processor);
            }
        }
        switch (list.size()) {
            case 0:
                return EmptyBatchProcessor.getInstance();
            case 1:
                return list.get(0);
            default:
                return new MultiBatchProcessor(list.toArray(new IBatchProcessor[0]));
        }
    }

    public void addBatchProcessor(IBatchProcessor processor) {
        List<IBatchProcessor> processors = new ArrayList<>(Arrays.asList(this.processors));
        processors.add(processor);
        this.processors = processors.toArray(new IBatchProcessor[0]);
    }

    public List<IBatchProcessor> getBatchProcessors() {
        return Arrays.asList(this.processors);
    }

    public void removeBatchProcessor(IBatchProcessor processor) {
        List<IBatchProcessor> processors = new ArrayList<>(Arrays.asList(this.processors));
        processors.remove(processor);
        this.processors = processors.toArray(new IBatchProcessor[0]);
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        try {
            IChunkSet chunkSet = set;
            for (int i = processors.length - 1; i >= 0; i--) {
                IBatchProcessor processor = processors[i];
                if (processor instanceof Filter) {
                    chunkSet = ((IBatchProcessor) classToThreadIdToFilter.getUnchecked(processor.getClass())
                        .computeIfAbsent(Thread.currentThread().getId(), k -> ((Filter) processor).fork())).processSet(chunk, get, chunkSet);
                } else {
                    chunkSet = processor.processSet(chunk, get, chunkSet);
                }
                if (chunkSet == null) {
                    return null;
                }
            }
            return chunkSet;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Future<IChunkSet> postProcessSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        try {
            for (int i = processors.length - 1 ; i >= 0; i--) {
                IBatchProcessor processor = processors[i];
                set = processor.postProcessSet(chunk, get, set).get();
                if (set == null) {
                    return null;
                }
            }
            return CompletableFuture.completedFuture(set);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean processGet(int chunkX, int chunkZ) {
        for (IBatchProcessor processor : this.processors) {
            if (!processor.processGet(chunkX, chunkZ)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Extent construct(Extent child) {
        for (IBatchProcessor processor : processors) {
            child = processor.construct(child);
        }
        return child;
    }

    @Override
    public <T extends IBatchProcessor> IBatchProcessor remove(Class<T> clazz) {
        ArrayList<IBatchProcessor> list = new ArrayList<>(Arrays.asList(this.processors));
        list.removeIf(clazz::isInstance);
        return of(list.toArray(new IBatchProcessor[0]));
    }

    @Override
    public IBatchProcessor join(IBatchProcessor other) {
        if (other instanceof MultiBatchProcessor) {
            for (IBatchProcessor processor : ((MultiBatchProcessor) other).processors) {
                addBatchProcessor(processor);
            }
        } else {
            addBatchProcessor(other);
        }
        return this;
    }

    @Override
    public IBatchProcessor joinPost(IBatchProcessor other) {
        if (other instanceof MultiBatchProcessor) {
            for (IBatchProcessor processor : ((MultiBatchProcessor) other).processors) {
                addBatchProcessor(processor);
            }
        } else {
            addBatchProcessor(other);
        }
        return this;
    }

    @Override
    public void flush() {
        for (IBatchProcessor processor : this.processors) {
            processor.flush();
        }
    }

    @Override
    public String toString() {
        return super.toString() + "{" + StringMan.join(processors, ",") + "}";
    }
}
