package com.fastasyncworldedit.core.util.io;

import com.fastasyncworldedit.core.util.MathMan;
import org.jetbrains.annotations.Range;

import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

final class MemoryFileSupport {

    /**
     * The number of additional bytes required to safely call {@link java.nio.ByteBuffer#getInt(int)}
     * and {@link java.nio.ByteBuffer#putInt(int, int)} for the last actually used byte.
     */
    static final int PADDING = 3;
    static final Set<? extends OpenOption> OPTIONS = EnumSet.of(
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.READ
    );

    static long requiredBytes(int bitsPerEntry, long entries) {
        long bitsNeeded = bitsPerEntry * entries;
        return MathMan.ceilDiv(bitsNeeded, 8) + MemoryFileSupport.PADDING;
    }

    static int bitsPerEntry(int valueCount) {
        if (Integer.highestOneBit(valueCount) == Integer.lowestOneBit(valueCount)) {
            return Integer.numberOfTrailingZeros(valueCount);
        }
        return MathMan.log2nlz(valueCount);
    }

    // Calculate the shift required for this bitPos
    static @Range(from = 0, to = 7) int shift(long bitPos, long bytePos) {
        return (int) (bitPos - (bytePos << 3));
    }

}
