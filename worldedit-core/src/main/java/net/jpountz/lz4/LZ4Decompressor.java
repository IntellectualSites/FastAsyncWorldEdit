package net.jpountz.lz4;



/**
 * @deprecated Use {@link LZ4FastDecompressor} instead.
 */
@Deprecated
public interface LZ4Decompressor {

    int decompress(byte[] src, int srcOff, byte[] dest, int destOff, int destLen);

}
