package com.boydti.fawe.object.task;

public interface ThrowableSupplier<T extends Throwable> {
    Object get() throws T;
}
