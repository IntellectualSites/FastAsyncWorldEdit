//CHECKSTYLE:OFF

package net.jpountz.util;

import java.nio.ByteOrder;

@SuppressWarnings("CheckStyle")
public enum Utils {
    ;

    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    private static final boolean unalignedAccessAllowed;

    static {
        String arch = System.getProperty("os.arch");
        unalignedAccessAllowed = arch.equals("i386") || arch.equals("x86") || arch.equals("amd64")
            || arch.equals("x86_64");
    }

    public static boolean isUnalignedAccessAllowed() {
        return unalignedAccessAllowed;
    }

}
