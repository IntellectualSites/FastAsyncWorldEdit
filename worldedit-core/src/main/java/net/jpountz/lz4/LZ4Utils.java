package net.jpountz.lz4;



import static net.jpountz.lz4.LZ4Constants.HASH_LOG;
import static net.jpountz.lz4.LZ4Constants.HASH_LOG_64K;
import static net.jpountz.lz4.LZ4Constants.HASH_LOG_HC;
import static net.jpountz.lz4.LZ4Constants.MIN_MATCH;

public enum LZ4Utils {
    ;

    private static final int MAX_INPUT_SIZE = 0x7E000000;

    public static int maxCompressedLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0, got " + length);
        } else if (length >= MAX_INPUT_SIZE) {
            throw new IllegalArgumentException("length must be < " + MAX_INPUT_SIZE);
        }
        return length + length / 255 + 16;
    }

    public static int hash(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG);
    }

    public static int hash64k(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG_64K);
    }

    public static int hashHC(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG_HC);
    }

    public static class Match {
        int start, ref, len;

        void fix(int correction) {
            start += correction;
            ref += correction;
            len -= correction;
        }

        int end() {
            return start + len;
        }
    }

    public static void copyTo(Match m1, Match m2) {
        m2.len = m1.len;
        m2.start = m1.start;
        m2.ref = m1.ref;
    }

}
