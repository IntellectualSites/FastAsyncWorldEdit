package com.fastasyncworldedit.core.util.task;


import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

@ApiStatus.Internal
public class FaweBasicThreadFactory implements ThreadFactory {

    private final String nameFormat;
    private final AtomicLong count;

    public FaweBasicThreadFactory(@Nullable String nameFormat) {
        this.nameFormat = nameFormat;
        this.count = (nameFormat != null) ? new AtomicLong(0) : null;
    }

    @Override
    public Thread newThread(@Nonnull final Runnable runnable) {
        Thread thread = new FaweBasicThread(runnable);
        if (nameFormat != null) {
            // requireNonNull is safe because we create `count` if (and only if) we have a nameFormat.
            thread.setName(String.format(Locale.ROOT, nameFormat, count.getAndIncrement()));
        }
        if (thread.isDaemon())
            thread.setDaemon(false);
        if (thread.getPriority() != Thread.NORM_PRIORITY)
            thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }

}
