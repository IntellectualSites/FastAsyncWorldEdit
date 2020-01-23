package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MainUtil;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CleanableThreadLocal<T> extends ThreadLocal<T> implements Closeable {
    private final Supplier<T> supplier;
    private final Function<T, T> modifier;
    private LongAdder count = new LongAdder();

    public CleanableThreadLocal(Supplier<T> supplier) {
        this(supplier, Function.identity());
    }

    public CleanableThreadLocal(Supplier<T> supplier, Consumer<T> modifier) {
        this(supplier, t -> {
            modifier.accept(t);
            return t;
        });
    }

    public CleanableThreadLocal(Supplier<T> supplier, Function<T, T> modifier) {
        this.supplier = supplier;
        this.modifier = modifier;
    }

    @Override
    protected final T initialValue() {
        T value = modifier.apply(init());
        if (value != null) {
            count.increment();
        }
        return value;
    }

    public T init() {
        return supplier.get();
    }

    public void clean() {
        if (count.sumThenReset() > 0) {
            CleanableThreadLocal.clean(this);
        }
    }

    public List<T> getAll() {
        List<T> list = new ArrayList<>();
        iterate(this, new Consumer<Object>() {
            Method methodGetEntry;
            Field fieldValue;
            @Override
            public void accept(Object tlm) {
                try {
                    if (methodGetEntry == null) {
                        methodGetEntry = tlm.getClass().getDeclaredMethod("getEntry", ThreadLocal.class);
                        methodGetEntry.setAccessible(true);
                    }
                    Object entry = methodGetEntry.invoke(tlm, CleanableThreadLocal.this);
                    if (entry != null) {
                        if (fieldValue == null) {
                            fieldValue = entry.getClass().getDeclaredField("value");
                            fieldValue.setAccessible(true);
                        }
                        Object value = fieldValue.get(entry);
                        if (value != null) {
                            list.add((T) value);
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return list;
    }

    public static void iterate(ThreadLocal instance, Consumer<Object> withMap) {
        try {
            Thread[] threads = MainUtil.getThreads();
            Field tl = Thread.class.getDeclaredField("threadLocals");
            tl.setAccessible(true);
            for (Thread thread : threads) {
                if (thread != null) {
                    Object tlm = tl.get(thread);
                    if (tlm != null) {
                        withMap.accept(tlm);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clean(ThreadLocal instance) {
        iterate(instance, new Consumer<Object>() {
            Method methodRemove;
            @Override
            public void accept(Object tlm) {
                try {
                    if (methodRemove == null) {
                        methodRemove = tlm.getClass().getDeclaredMethod("remove", ThreadLocal.class);
                        methodRemove.setAccessible(true);
                    }
                    if (methodRemove != null) {
                        try {
                            methodRemove.invoke(tlm, instance);
                        } catch (Throwable ignore) {
                        }
                    }
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void cleanAll() {
        try {
            // Get a reference to the thread locals table of the current thread
            Thread thread = Thread.currentThread();
            Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalTable = threadLocalsField.get(thread);

            // Get a reference to the array holding the thread local variables inside the
            // ThreadLocalMap of the current thread
            Class<?> threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = threadLocalMapClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalTable);

            // The key to the ThreadLocalMap is a WeakReference object. The referent field of this object
            // is a reference to the actual ThreadLocal variable
            Field referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);

            for (int i = 0; i < Array.getLength(table); i++) {
                // Each entry in the table array of ThreadLocalMap is an Entry object
                // representing the thread local reference and its value
                Object entry = Array.get(table, i);
                if (entry != null) {
                    // Get a reference to the thread local object and remove it from the table
                    ThreadLocal threadLocal = (ThreadLocal)referentField.get(entry);
                    clean(threadLocal);
                }
            }
        } catch(Exception e) {
            // We will tolerate an exception here and just log it
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        clean(this);
        super.finalize();
    }

    @Override
    public void close() throws IOException {
        clean();
    }
}
