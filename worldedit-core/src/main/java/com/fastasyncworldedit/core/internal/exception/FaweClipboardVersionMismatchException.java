package com.fastasyncworldedit.core.internal.exception;

import com.fastasyncworldedit.core.configuration.Caption;

public class FaweClipboardVersionMismatchException extends FaweException {

    final int version;

    public FaweClipboardVersionMismatchException() {
        this(-1);
    }

    /**
     * New exception specifying a version mismatch between that supported and that loaded.
     *
     * @param version version of clipboard attempting to be loaded
     */
    public FaweClipboardVersionMismatchException(int version) {
        super(Caption.of("fawe.error.clipboard.on.disk.version.mismatch"), Type.CLIPBOARD);
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

}
