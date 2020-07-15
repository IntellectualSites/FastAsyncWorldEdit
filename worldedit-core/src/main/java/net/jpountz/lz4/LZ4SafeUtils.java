package net.jpountz.lz4;

import net.jpountz.util.SafeUtils;

import static net.jpountz.lz4.LZ4Constants.LAST_LITERALS;
import static net.jpountz.lz4.LZ4Constants.ML_BITS;
import static net.jpountz.lz4.LZ4Constants.ML_MASK;
import static net.jpountz.lz4.LZ4Constants.RUN_MASK;

@SuppressWarnings("CheckStyle")
enum LZ4SafeUtils {
    ;

    static int hash(byte[] buf, int i) {
        return LZ4Utils.hash(SafeUtils.readInt(buf, i));
    }

    static int hash64k(byte[] buf, int i) {
        return LZ4Utils.hash64k(SafeUtils.readInt(buf, i));
    }

    static boolean readIntEquals(byte[] buf, int i, int j) {
        return buf[i] == buf[j] && buf[i + 1] == buf[j + 1] && buf[i + 2] == buf[j + 2]
            && buf[i + 3] == buf[j + 3];
    }

    static void safeIncrementalCopy(byte[] dest, int matchOff, int dOff, int matchLen) {
        if (matchLen >= 0) {
            System.arraycopy(dest, matchOff, dest, dOff, matchLen);
        }
    }

    static void wildIncrementalCopy(byte[] dest, int matchOff, int dOff, int matchCopyEnd) {
        do {
            copy8Bytes(dest, matchOff, dest, dOff);
            matchOff += 8;
            dOff += 8;
        } while (dOff < matchCopyEnd);
    }

    static void copy8Bytes(byte[] src, int sOff, byte[] dest, int dOff) {
        System.arraycopy(src, sOff, dest, dOff, 8);
    }

    static int commonBytes(byte[] b, int o1, int o2, int limit) {
        int count = 0;
        while (o2 < limit && b[o1++] == b[o2++]) {
            ++count;
        }
        return count;
    }

    static int commonBytesBackward(byte[] b, int o1, int o2, int l1, int l2) {
        int count = 0;
        while (o1 > l1 && o2 > l2 && b[--o1] == b[--o2]) {
            ++count;
        }
        return count;
    }

    static void safeArraycopy(byte[] src, int sOff, byte[] dest, int dOff, int len) {
        System.arraycopy(src, sOff, dest, dOff, len);
    }

    static void wildArraycopy(byte[] src, int sOff, byte[] dest, int dOff, int len) {
        try {
            for (int i = 0; i < len; i += 8) {
                copy8Bytes(src, sOff + i, dest, dOff + i);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new LZ4Exception("Malformed input at offset " + sOff);
        }
    }

    static int encodeSequence(byte[] src, int anchor, int matchOff, int matchRef, int matchLen, byte[] dest, int dOff, int destEnd) {
        final int runLen = matchOff - anchor;
        final int tokenOff = dOff++;

        if (dOff + runLen + (2 + 1 + LAST_LITERALS) + (runLen >>> 8) > destEnd) {
            throw new LZ4Exception("maxDestLen is too small");
        }

        int token;
        if (runLen >= RUN_MASK) {
            token = (byte) (RUN_MASK << ML_BITS);
            dOff = writeLen(runLen - RUN_MASK, dest, dOff);
        } else {
            token = runLen << ML_BITS;
        }

        // copy literals
        wildArraycopy(src, anchor, dest, dOff, runLen);
        dOff += runLen;

        // encode offset
        final int matchDec = matchOff - matchRef;
        dest[dOff++] = (byte) matchDec;
        dest[dOff++] = (byte) (matchDec >>> 8);

        // encode match len
        matchLen -= 4;
        if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
            throw new LZ4Exception("maxDestLen is too small");
        }
        if (matchLen >= ML_MASK) {
            token |= ML_MASK;
            dOff = writeLen(matchLen - RUN_MASK, dest, dOff);
        } else {
            token |= matchLen;
        }

        dest[tokenOff] = (byte) token;

        return dOff;
    }

    static int lastLiterals(byte[] src, int sOff, int srcLen, byte[] dest, int dOff, int destEnd) {

        if (dOff + srcLen + 1 + (srcLen + 255 - RUN_MASK) / 255 > destEnd) {
            throw new LZ4Exception();
        }

        if (srcLen >= RUN_MASK) {
            dest[dOff++] = (byte) (RUN_MASK << ML_BITS);
            dOff = writeLen(srcLen - RUN_MASK, dest, dOff);
        } else {
            dest[dOff++] = (byte) (srcLen << ML_BITS);
        }
        // copy literals
        System.arraycopy(src, sOff, dest, dOff, srcLen);
        dOff += srcLen;

        return dOff;
    }

    static int writeLen(int len, byte[] dest, int dOff) {
        while (len >= 0xFF) {
            dest[dOff++] = (byte) 0xFF;
            len -= 0xFF;
        }
        dest[dOff++] = (byte) len;
        return dOff;
    }

    static void copyTo(Match m1, Match m2) {
        m2.len = m1.len;
        m2.start = m1.start;
        m2.ref = m1.ref;
    }


    static class Match {
        int start;
        int ref;
        int len;

        void fix(int correction) {
            start += correction;
            ref += correction;
            len -= correction;
        }

        int end() {
            return start + len;
        }
    }

}
