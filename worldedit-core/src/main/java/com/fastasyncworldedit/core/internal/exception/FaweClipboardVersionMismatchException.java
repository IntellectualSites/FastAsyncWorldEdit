package com.fastasyncworldedit.core.internal.exception;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard;

public class FaweClipboardVersionMismatchException extends FaweException {

    private final int expected;
    private final int version;

    /**
     * @deprecated Use {@link FaweClipboardVersionMismatchException#FaweClipboardVersionMismatchException(int, int)}
     */
    @Deprecated(forRemoval = true, since = "TODO")
    public FaweClipboardVersionMismatchException() {
        this(DiskOptimizedClipboard.VERSION, -1);
    }

    /**
     * New exception specifying a version mismatch between that supported and that loaded.
     *
     * @param version  version of clipboard attempting to be loaded
     * @param expected expected version of clipboard
     */
    public FaweClipboardVersionMismatchException(int expected, int version) {
        super(Caption.of("fawe.error.clipboard.on.disk.version.mismatch", expected, version), Type.CLIPBOARD);
        this.expected = expected;
        this.version = version;
    }

    /**
     * Get the version specified in the clipboard attempting to be loaded.
     */
    public int getClipboardVersion() {
        return version;
    }

    /**
     * Get the version that was expected of the clipboard
     */
    public int getExpectedVersion() {
        return expected;
    }
}
