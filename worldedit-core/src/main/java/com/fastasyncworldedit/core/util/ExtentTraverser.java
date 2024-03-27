package com.fastasyncworldedit.core.util;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class ExtentTraverser<T extends Extent> {

    private final T root;
    private final ExtentTraverser<T> parent;

    public ExtentTraverser(@Nonnull T root) {
        this(root, null);
    }

    public ExtentTraverser(@Nonnull T root, ExtentTraverser<T> parent) {
        this.root = root;
        this.parent = parent;
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
            if (root instanceof AbstractDelegateExtent) {
                AbstractDelegateExtent root = (AbstractDelegateExtent) this.root;
                T value = (T) root.getExtent();
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

}
