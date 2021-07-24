package com.fastasyncworldedit.core.util.task;

public interface ThrowableSupplier<T extends Throwable> {

    Object get() throws T;

}
