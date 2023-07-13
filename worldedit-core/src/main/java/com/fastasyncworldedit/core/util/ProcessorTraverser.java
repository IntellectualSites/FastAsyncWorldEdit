package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.extent.processor.BatchProcessorHolder;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.queue.IBatchProcessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;

public class ProcessorTraverser<T extends IBatchProcessor> {

    private final T root;

    public ProcessorTraverser(@Nonnull T root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public @Nullable <U extends IBatchProcessor> U find(Class<U> clazz) {
        try {
            Queue<IBatchProcessor> processors = new ArrayDeque<>(5);
            if (this.root instanceof MultiBatchProcessor multiBatchProcessor) {
                processors.addAll(multiBatchProcessor.getBatchProcessors());
            } else if (this.root instanceof BatchProcessorHolder holder) {
                processors.add(holder.getProcessor());
            } else {
                return clazz.isAssignableFrom(this.root.getClass()) ? clazz.cast(this.root) : null;
            }
            IBatchProcessor processor;
            while ((processor = processors.poll()) != null) {
                if (clazz.isAssignableFrom(processor.getClass())) {
                    return (U) processor;
                } else if (processor instanceof MultiBatchProcessor multiProcessor) {
                    processors.addAll(multiProcessor.getBatchProcessors());
                } else if (processor instanceof BatchProcessorHolder holder) {
                    processors.add(holder.getProcessor());
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

}
