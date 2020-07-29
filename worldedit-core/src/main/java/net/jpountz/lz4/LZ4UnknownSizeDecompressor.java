package net.jpountz.lz4;



/**
 * @deprecated Use {@link LZ4SafeDecompressor} instead.
 */
@Deprecated
public interface LZ4UnknownSizeDecompressor {

    int decompress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen);

    int decompress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff);

}
