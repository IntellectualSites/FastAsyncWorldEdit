package com.fastasyncworldedit.object.task;

public interface ThrowableSupplier<T extends Throwable> {
    Object get() throws T;
}
