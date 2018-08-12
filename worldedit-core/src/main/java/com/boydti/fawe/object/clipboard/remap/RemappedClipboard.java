package com.boydti.fawe.object.clipboard.remap;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.clipboard.AbstractDelegateFaweClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

// TODO FIXME
public class RemappedClipboard extends AbstractDelegateFaweClipboard {
    private final ClipboardRemapper remapper;

    public RemappedClipboard(FaweClipboard parent, ClipboardRemapper remapper) {
        super(parent);
        this.remapper = remapper;
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return (BlockState) remapper.remap(super.getBlock(x, y, z));
    }

    @Override
    public BlockState getBlock(int index) {
        return (BlockState) remapper.remap(super.getBlock(index));
    }

    @Override
    public void forEach(BlockReader task, boolean air) {
        super.forEach(new BlockReader() {
            @Override
            public void run(int x, int y, int z, BlockState block) {
                task.run(x, y, z, (BlockState) remapper.remap(block));
            }
        }, air);
    }

    @Override
    public void streamCombinedIds(NBTStreamer.ByteReader task) {
        super.streamCombinedIds(task);
    }
//
//    @Override
//    public void streamIds(NBTStreamer.ByteReader task) {
//        super.streamIds(new NBTStreamer.ByteReader() {
//            @Override
//            public void run(int index, int byteValue) {
//                if (remapper.hasRemapId(byteValue)) {
//                    int result = remapper.remapId(byteValue);
//                    if (result != byteValue) {
//                        task.run(index, result);
//                    } else {
//                        task.run(index, getBlock(index).getId());
//                    }
//                }
//            }
//        });
//    }
//
//    @Override
//    public void streamDatas(NBTStreamer.ByteReader task) {
//        super.streamDatas(new NBTStreamer.ByteReader() {
//            @Override
//            public void run(int index, int byteValue) {
//                if (remapper.hasRemapData(byteValue)) {
//                    task.run(index, getBlock(index).getData());
//                }
//            }
//        });
//    }
}
