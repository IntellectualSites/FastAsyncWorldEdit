// Auto-generated: DO NOT EDIT

package net.jpountz.lz4;

import java.nio.ByteBuffer;
import net.jpountz.util.ByteBufferUtils;
import net.jpountz.util.UnsafeUtils;


import static net.jpountz.lz4.LZ4Constants.*;

/**
 * Decompressor.
 */
final class LZ4JavaUnsafeFastDecompressor extends LZ4FastDecompressor {

    public static final LZ4FastDecompressor INSTANCE = new LZ4JavaUnsafeFastDecompressor();

    @Override
    public int decompress(byte[] src, final int srcOff, byte[] dest, final int destOff, int destLen) {


        UnsafeUtils.checkRange(src, srcOff);
        UnsafeUtils.checkRange(dest, destOff, destLen);

        if (destLen == 0) {
            if (UnsafeUtils.readByte(src, srcOff) != 0) {
                throw new LZ4Exception("Malformed input at " + srcOff);
            }
            return 1;
        }


        final int destEnd = destOff + destLen;

        int sOff = srcOff;
        int dOff = destOff;

        while (true) {
            final int token = UnsafeUtils.readByte(src, sOff) & 0xFF;
            ++sOff;

            // literals
            int literalLen = token >>> ML_BITS;
            if (literalLen == RUN_MASK) {
                byte len = (byte) 0xFF;
                while ((len = UnsafeUtils.readByte(src, sOff++)) == (byte) 0xFF) {
                    literalLen += 0xFF;
                }
                literalLen += len & 0xFF;
            }

            final int literalCopyEnd = dOff + literalLen;

            if (literalCopyEnd > destEnd - COPY_LENGTH) {
                if (literalCopyEnd != destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);

                } else {
                    LZ4UnsafeUtils.safeArraycopy(src, sOff, dest, dOff, literalLen);
                    sOff += literalLen;
                    dOff = literalCopyEnd;
                    break; // EOF
                }
            }

            LZ4UnsafeUtils.wildArraycopy(src, sOff, dest, dOff, literalLen);
            sOff += literalLen;
            dOff = literalCopyEnd;

            // matchs
            final int matchDec = UnsafeUtils.readShortLE(src, sOff);
            sOff += 2;
            int matchOff = dOff - matchDec;

            if (matchOff < destOff) {
                throw new LZ4Exception("Malformed input at " + sOff);
            }

            int matchLen = token & ML_MASK;
            if (matchLen == ML_MASK) {
                byte len = (byte) 0xFF;
                while ((len = UnsafeUtils.readByte(src, sOff++)) == (byte) 0xFF) {
                    matchLen += 0xFF;
                }
                matchLen += len & 0xFF;
            }
            matchLen += MIN_MATCH;

            final int matchCopyEnd = dOff + matchLen;

            if (matchCopyEnd > destEnd - COPY_LENGTH) {
                if (matchCopyEnd > destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);
                }
                LZ4UnsafeUtils.safeIncrementalCopy(dest, matchOff, dOff, matchLen);
            } else {
                LZ4UnsafeUtils.wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);
            }
            dOff = matchCopyEnd;
        }


        return sOff - srcOff;

    }

    @Override
    public int decompress(ByteBuffer src, final int srcOff, ByteBuffer dest, final int destOff, int destLen) {

        if (src.hasArray() && dest.hasArray()) {
            return decompress(src.array(), srcOff + src.arrayOffset(), dest.array(), destOff + dest.arrayOffset(), destLen);
        }
        src = ByteBufferUtils.inNativeByteOrder(src);
        dest = ByteBufferUtils.inNativeByteOrder(dest);


        ByteBufferUtils.checkRange(src, srcOff);
        ByteBufferUtils.checkRange(dest, destOff, destLen);

        if (destLen == 0) {
            if (ByteBufferUtils.readByte(src, srcOff) != 0) {
                throw new LZ4Exception("Malformed input at " + srcOff);
            }
            return 1;
        }


        final int destEnd = destOff + destLen;

        int sOff = srcOff;
        int dOff = destOff;

        while (true) {
            final int token = ByteBufferUtils.readByte(src, sOff) & 0xFF;
            ++sOff;

            // literals
            int literalLen = token >>> ML_BITS;
            if (literalLen == RUN_MASK) {
                byte len = (byte) 0xFF;
                while ((len = ByteBufferUtils.readByte(src, sOff++)) == (byte) 0xFF) {
                    literalLen += 0xFF;
                }
                literalLen += len & 0xFF;
            }

            final int literalCopyEnd = dOff + literalLen;

            if (literalCopyEnd > destEnd - COPY_LENGTH) {
                if (literalCopyEnd != destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);

                } else {
                    LZ4ByteBufferUtils.safeArraycopy(src, sOff, dest, dOff, literalLen);
                    sOff += literalLen;
                    dOff = literalCopyEnd;
                    break; // EOF
                }
            }

            LZ4ByteBufferUtils.wildArraycopy(src, sOff, dest, dOff, literalLen);
            sOff += literalLen;
            dOff = literalCopyEnd;

            // matchs
            final int matchDec = ByteBufferUtils.readShortLE(src, sOff);
            sOff += 2;
            int matchOff = dOff - matchDec;

            if (matchOff < destOff) {
                throw new LZ4Exception("Malformed input at " + sOff);
            }

            int matchLen = token & ML_MASK;
            if (matchLen == ML_MASK) {
                byte len = (byte) 0xFF;
                while ((len = ByteBufferUtils.readByte(src, sOff++)) == (byte) 0xFF) {
                    matchLen += 0xFF;
                }
                matchLen += len & 0xFF;
            }
            matchLen += MIN_MATCH;

            final int matchCopyEnd = dOff + matchLen;

            if (matchCopyEnd > destEnd - COPY_LENGTH) {
                if (matchCopyEnd > destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);
                }
                LZ4ByteBufferUtils.safeIncrementalCopy(dest, matchOff, dOff, matchLen);
            } else {
                LZ4ByteBufferUtils.wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);
            }
            dOff = matchCopyEnd;
        }


        return sOff - srcOff;

    }


}

