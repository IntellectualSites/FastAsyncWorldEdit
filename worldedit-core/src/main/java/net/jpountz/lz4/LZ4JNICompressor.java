package net.jpountz.lz4;

import java.nio.ByteBuffer;

import static net.jpountz.util.ByteBufferUtils.checkNotReadOnly;
import static net.jpountz.util.ByteBufferUtils.checkRange;
import static net.jpountz.util.SafeUtils.checkRange;

/**
 * Fast {@link LZ4Compressor} implemented with JNI bindings to the original C
 * implementation of LZ4.
 */
final class LZ4JNICompressor extends LZ4Compressor {

    public static final LZ4Compressor INSTANCE = new LZ4JNICompressor();
    private static LZ4Compressor SAFE_INSTANCE;

    @Override
    public int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen) {
        checkRange(src, srcOff, srcLen);
        checkRange(dest, destOff, maxDestLen);
        final int result = LZ4JNI.LZ4_compress_limitedOutput(src, null, srcOff, srcLen, dest, null, destOff, maxDestLen);
        if (result <= 0) {
            throw new LZ4Exception("maxDestLen is too small");
        }
        return result;
    }

    @Override
    public int compress(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dest, int destOff, int maxDestLen) {
        checkNotReadOnly(dest);
        checkRange(src, srcOff, srcLen);
        checkRange(dest, destOff, maxDestLen);

        if ((src.hasArray() || src.isDirect()) && (dest.hasArray() || dest.isDirect())) {
            byte[] srcArr = null;
            byte[] destArr = null;
            ByteBuffer srcBuf = null;
            ByteBuffer destBuf = null;
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

            final int result = LZ4JNI.LZ4_compress_limitedOutput(srcArr, srcBuf, srcOff, srcLen, destArr, destBuf, destOff, maxDestLen);
            if (result <= 0) {
                throw new LZ4Exception("maxDestLen is too small");
            }
            return result;
        } else {
            LZ4Compressor safeInstance = SAFE_INSTANCE;
            if (safeInstance == null) {
                safeInstance = SAFE_INSTANCE = LZ4Factory.safeInstance().fastCompressor();
            }
            return safeInstance.compress(src, srcOff, srcLen, dest, destOff, maxDestLen);
        }
    }
}
