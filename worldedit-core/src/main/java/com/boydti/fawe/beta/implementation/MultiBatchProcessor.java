package com.boydti.fawe.beta.implementation;

import com.boydti.fawe.beta.IBatchProcessor;
import com.boydti.fawe.beta.IChunk;
import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.IChunkSet;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.extent.Extent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiBatchProcessor implements IBatchProcessor {
    private IBatchProcessor[] processors;

    public MultiBatchProcessor(IBatchProcessor... processors) {
        this.processors = processors;
    }

    public static IBatchProcessor of(IBatchProcessor... processors) {
        ArrayList<IBatchProcessor> list = new ArrayList<>();
        for (IBatchProcessor processor : processors) {
            if (processor instanceof MultiBatchProcessor) {
                list.addAll(Arrays.asList(((MultiBatchProcessor) processor).processors));
            } else if (!(processor instanceof EmptyBatchProcessor)){
                list.add(processor);
            }
        }
        switch (list.size()) {
            case 0:
                return EmptyBatchProcessor.INSTANCE;
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
            for (IBatchProcessor processor : this.processors) {
                set = processor.processSet(chunk, get, set);
                if (set == null) {
                    return null;
                }
            }
            return set;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
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
    public String toString() {
        return super.toString() + "{" + StringMan.join(processors, ",") + "}";
    }
}
