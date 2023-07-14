package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.extent.processor.BatchProcessorHolder;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;

public class ProcessorTraverser<T extends IBatchProcessor> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final T root;

    public ProcessorTraverser(@Nonnull T root) {
        this.root = root;
    }

    public <U extends IBatchProcessor> @Nullable U find(Class<U> clazz) {
        try {
            Queue<IBatchProcessor> processors = new ArrayDeque<>();
            IBatchProcessor processor = root;
            do {
                if (clazz.isAssignableFrom(processor.getClass())) {
                    return clazz.cast(processor);
                } else if (processor instanceof MultiBatchProcessor multiProcessor) {
                    processors.addAll(multiProcessor.getBatchProcessors());
                } else if (processor instanceof BatchProcessorHolder holder) {
                    processors.add(holder.getProcessor());
                }
            } while ((processor = processors.poll()) != null);
            return null;
        } catch (Throwable e) {
            LOGGER.error("Error traversing processors", e);
            return null;
        }
    }

}
