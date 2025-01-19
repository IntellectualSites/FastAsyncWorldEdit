package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.fastasyncworldedit.core.queue.implementation.SingleThreadQueueExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.world.World;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class ExtentTraverser<T extends Extent> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final T root;
    private final ExtentTraverser<T> parent;

    public ExtentTraverser(@Nonnull T root) {
        this(root, null);
    }

    public ExtentTraverser(@Nonnull T root, ExtentTraverser<T> parent) {
        this.root = root;
        this.parent = parent;
    }

    /**
     * Get the world backing the given extent, if present, else null.
     *
     * @since 2.12.3
     */
    @Nullable
    public static World getWorldFromExtent(Extent extent) {
        if (extent.isWorld()) {
            return (World) extent;
        } else if (extent instanceof EditSession session) {
            return session.getWorld();
        } else if (extent instanceof SingleThreadQueueExtent stqe) {
            return stqe.getWorld();
        } else if (extent instanceof ParallelQueueExtent pqe) {
            return ((SingleThreadQueueExtent) pqe.getExtent()).getWorld();
        } else {
            return new ExtentTraverser<>(extent).findAndGet(World.class);
        }
    }

    public boolean exists() {
        return root != null;
    }

    @Nullable
    public T get() {
        return root;
    }

    public boolean setNext(T next) {
        try {
            Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            ReflectionUtils.setFailsafeFieldValue(field, root, next);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public ExtentTraverser<T> last() {
        ExtentTraverser<T> last = this;
        ExtentTraverser<T> traverser = this;
        while (traverser != null && traverser.get() instanceof AbstractDelegateExtent) {
            last = traverser;
            traverser = traverser.next();
        }
        return last;
    }

    @Nullable
    public <U extends Extent> U findAndGet(Class<U> clazz) {
        ExtentTraverser<U> traverser = find(clazz);
        return (traverser != null) ? traverser.get() : null;
    }

    @SuppressWarnings("unchecked")
    public <U extends Extent> ExtentTraverser<U> find(Class<U> clazz) {
        try {
            ExtentTraverser<T> value = this;
            while (value != null) {
                if (clazz.isAssignableFrom(value.root.getClass())) {
                    return (ExtentTraverser<U>) value;
                }
                value = value.next();
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <U extends Extent> ExtentTraverser<U> find(Object object) {
        try {
            ExtentTraverser<T> value = this;
            while (value != null) {
                if (value.root == object) {
                    return (ExtentTraverser<U>) value;
                }
                value = value.next();
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public ExtentTraverser<T> previous() {
        return parent;
    }

    @SuppressWarnings("unchecked")
    public ExtentTraverser<T> next() {
        try {
            if (root instanceof AbstractDelegateExtent abstractDelegateExtent) {
                T value = (T) abstractDelegateExtent.getExtent();
                if (value == null) {
                    return null;
                }
                return new ExtentTraverser<>(value, this);
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void printNestedExtents(Extent extent) {
        String nested = printNestedExtent(new StringBuilder("Extent tree:"), new ExtentTraverser<>(extent), 0).toString();
        LOGGER.info(nested);
    }

    private static StringBuilder printNestedExtent(StringBuilder builder, ExtentTraverser<?> traverser, int depth) {
        if (traverser == null || !traverser.exists()) {
            return builder;
        }
        String indent = "  ".repeat(Math.max(0, depth)); // Adjust the indentation as needed

        Extent extent = traverser.get();
        builder.append("\n").append(indent).append("- ").append(extent.getClass().getSimpleName());

        // Recursively print nested extents
        printNestedExtent(builder, traverser.next(), depth + 1);
        return builder;
    }

}
