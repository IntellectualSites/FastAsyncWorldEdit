package net.jpountz.lz4;



import net.jpountz.util.ByteBufferUtils;
import net.jpountz.util.SafeUtils;

import java.nio.ByteBuffer;

import static net.jpountz.lz4.LZ4Constants.DEFAULT_COMPRESSION_LEVEL;

/**
 * High compression {@link LZ4Compressor}s implemented with JNI bindings to the
 * original C implementation of LZ4.
 */
final class LZ4HCJNICompressor extends LZ4Compressor {

    public static final LZ4HCJNICompressor INSTANCE = new LZ4HCJNICompressor();
    private static LZ4Compressor SAFE_INSTANCE;

    private final int compressionLevel;

    LZ4HCJNICompressor() {
        this(DEFAULT_COMPRESSION_LEVEL);
    }

    LZ4HCJNICompressor(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    @Override
    public int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen) {
        SafeUtils.checkRange(src, srcOff, srcLen);
        SafeUtils.checkRange(dest, destOff, maxDestLen);
        final int result = LZ4JNI.LZ4_compressHC(src, null, srcOff, srcLen, dest, null, destOff, maxDestLen, compressionLevel);
        if (result <= 0) {
            throw new LZ4Exception();
        }
        return result;
    }

    @Override
    public int compress(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dest, int destOff, int maxDestLen) {
        ByteBufferUtils.checkNotReadOnly(dest);
        ByteBufferUtils.checkRange(src, srcOff, srcLen);
        ByteBufferUtils.checkRange(dest, destOff, maxDestLen);

        if ((src.hasArray() || src.isDirect()) && (dest.hasArray() || dest.isDirect())) {
            byte[] srcArr = null, destArr = null;
            ByteBuffer srcBuf = null, destBuf = null;
            if (src.hasArray()) {
                srcArr = src.array();
                srcOff += src.arrayOffset();
            } else {
                assert src.isDirect();
                srcBuf = src;
            }
            if (dest.hasArray()) {
                destArr = dest.array();
                destOff += dest.arrayOffset();
            } else {
                assert dest.isDirect();
                destBuf = dest;
            }

            final int result = LZ4JNI.LZ4_compressHC(srcArr, srcBuf, srcOff, srcLen, destArr, destBuf, destOff, maxDestLen, compressionLevel);
            if (result <= 0) {
                throw new LZ4Exception();
            }
            return result;
        } else {
            LZ4Compressor safeInstance = SAFE_INSTANCE;
            if (safeInstance == null) {
                safeInstance = SAFE_INSTANCE = LZ4Factory.safeInstance().highCompressor(compressionLevel);
            }
            return safeInstance.compress(src, srcOff, srcLen, dest, destOff, maxDestLen);
        }
    }
}
