package com.fastasyncworldedit.core.internal.exception;

import com.fastasyncworldedit.core.configuration.Caption;

public class FaweClipboardVersionMismatchException extends FaweException {

    public FaweClipboardVersionMismatchException() {
        super(Caption.of("fawe.error.clipboard.on.disk.version.mismatch"), Type.CLIPBOARD);
    }

}
