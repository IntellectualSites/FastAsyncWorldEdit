package cn.nukkit.network.protocol;

public class UpdateBlockPacket extends DataPacket {

    public static final int FLAG_ALL = 0;
    public int x;
    public int y;
    public int z;
    public int flags;
    public int blockRuntimeId;
}
