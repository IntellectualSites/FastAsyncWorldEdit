package net.jpountz.lz4;



import net.jpountz.util.Native;

import java.nio.ByteBuffer;


/**
 * JNI bindings to the original C implementation of LZ4.
 */
enum LZ4JNI {
    ;

    static {
        Native.load();
        init();
    }

    static native void init();

    static native int LZ4_compress_limitedOutput(byte[] srcArray, ByteBuffer srcBuffer, int srcOff, int srcLen, byte[] destArray, ByteBuffer destBuffer, int destOff, int maxDestLen);

    static native int LZ4_compressHC(byte[] srcArray, ByteBuffer srcBuffer, int srcOff, int srcLen, byte[] destArray, ByteBuffer destBuffer, int destOff, int maxDestLen, int compressionLevel);

    static native int LZ4_decompress_fast(byte[] srcArray, ByteBuffer srcBuffer, int srcOff, byte[] destArray, ByteBuffer destBuffer, int destOff, int destLen);

    static native int LZ4_decompress_safe(byte[] srcArray, ByteBuffer srcBuffer, int srcOff, int srcLen, byte[] destArray, ByteBuffer destBuffer, int destOff, int maxDestLen);

    static native int LZ4_compressBound(int len);

}

