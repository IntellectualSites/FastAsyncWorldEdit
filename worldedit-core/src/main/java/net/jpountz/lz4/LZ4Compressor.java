package net.jpountz.lz4;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * LZ4 compressor.
 * <p>
 * Instances of this class are thread-safe.
 * </p>
 */
@SuppressWarnings("CheckStyle")
public abstract class LZ4Compressor {

    /**
     * Return the maximum compressed length for an input of size <code>length</code>.
     */
    @SuppressWarnings("static-method")
    public final int maxCompressedLength(int length) {
        return LZ4Utils.maxCompressedLength(length);
    }

    /**
     * Compress <code>src[srcOff:srcOff+srcLen]</code> into
     * <code>dest[destOff:destOff+destLen]</code> and return the compressed
     * length.
     * <p>
     * This method will throw a {@link LZ4Exception} if this compressor is unable
     * to compress the input into less than <code>maxDestLen</code> bytes. To
     * prevent this exception to be thrown, you should make sure that
     * <code>maxDestLen >= maxCompressedLength(srcLen)</code>.
     *
     * @return the compressed size
     * @throws LZ4Exception if maxDestLen is too small
     */
    public abstract int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen);

    /**
     * Compress <code>src[srcOff:srcOff+srcLen]</code> into
     * <code>dest[destOff:destOff+destLen]</code> and return the compressed
     * length.
     * <p>
     * This method will throw a {@link LZ4Exception} if this compressor is unable
     * to compress the input into less than <code>maxDestLen</code> bytes. To
     * prevent this exception to be thrown, you should make sure that
     * <code>maxDestLen >= maxCompressedLength(srcLen)</code>.
     * <p>
     * {@link ByteBuffer} positions remain unchanged.
     *
     * @return the compressed size
     * @throws LZ4Exception if maxDestLen is too small
     */
    public abstract int compress(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dest, int destOff, int maxDestLen);

    /**
     * Convenience method, equivalent to calling
     * {@link #compress(byte[], int, int, byte[], int, int) compress(src, srcOff, srcLen, dest, destOff, dest.length - destOff)}.
     */
    public final int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff) {
        return compress(src, srcOff, srcLen, dest, destOff, dest.length - destOff);
    }

    /**
     * Convenience method, equivalent to calling
     * {@link #compress(byte[], int, int, byte[], int) compress(src, 0, src.length, dest, 0)}.
     */
    public final int compress(byte[] src, byte[] dest) {
        return compress(src, 0, src.length, dest, 0);
    }

    /**
     * Convenience method which returns <code>src[srcOff:srcOff+srcLen]</code>
     * compressed.
     * <p><b><span style="color:red">Warning</span></b>: this method has an
     * important overhead due to the fact that it needs to allocate a buffer to
     * compress into, and then needs to resize this buffer to the actual
     * compressed length.</p>
     * <p>Here is how this method is implemented:</p>
     * <pre>
     * final int maxCompressedLength = maxCompressedLength(srcLen);
     * final byte[] compressed = new byte[maxCompressedLength];
     * final int compressedLength = compress(src, srcOff, srcLen, compressed, 0);
     * return Arrays.copyOf(compressed, compressedLength);
     * </pre>
     */
    public final byte[] compress(byte[] src, int srcOff, int srcLen) {
        final int maxCompressedLength = maxCompressedLength(srcLen);
        final byte[] compressed = new byte[maxCompressedLength];
        final int compressedLength = compress(src, srcOff, srcLen, compressed, 0);
        return Arrays.copyOf(compressed, compressedLength);
    }

    /**
     * Convenience method, equivalent to calling
     * {@link #compress(byte[], int, int) compress(src, 0, src.length)}.
     */
    public final byte[] compress(byte[] src) {
        return compress(src, 0, src.length);
    }

    /**
     * Compress <code>src</code> into <code>dest</code>. Calling this method
     * will update the positions of both {@link ByteBuffer}s.
     */
    public final void compress(ByteBuffer src, ByteBuffer dest) {
        final int cpLen = compress(src, src.position(), src.remaining(), dest, dest.position(), dest.remaining());
        src.position(src.limit());
        dest.position(dest.position() + cpLen);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
