package com.boydti.fawe.util;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.lang.reflect.Field;

public class ExtentTraverser<T extends Extent> {
    private T root;
    private ExtentTraverser<T> parent;

    public ExtentTraverser(T root) {
        this(root, null);
    }

    public ExtentTraverser(T root, ExtentTraverser<T> parent) {
        this.root = root;
        this.parent = parent;
    }

    public boolean exists() {
        return root != null;
    }

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

    public boolean insert(T extent) {
        try {
            Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            field.setAccessible(true);
            field.set(extent, field.get(root));
            field.set(root, extent);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public <U> U findAndGet(Class<U> clazz) {
        ExtentTraverser<Extent> traverser = find((Class) clazz);
        return (traverser != null) ? (U) traverser.get() : null;
    }

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
            MainUtil.handleError(e);
            return null;
        }
    }

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
            MainUtil.handleError(e);
            return null;
        }
    }

    public ExtentTraverser<T> previous() {
        return parent;
    }

    public ExtentTraverser<T> next() {
        try {
            if (root instanceof AbstractDelegateExtent) {
                Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
                field.setAccessible(true);
                T value = (T) field.get(root);
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
