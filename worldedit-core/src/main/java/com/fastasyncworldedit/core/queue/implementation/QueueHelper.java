package com.fastasyncworldedit.core.queue.implementation;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.queue.IChunkCache;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.implementation.blocks.CharSetBlocks;
import com.fastasyncworldedit.core.queue.implementation.blocks.IntSetBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class QueueHelper {

    @NotNull
    protected static IChunkCache<IChunkSet> getIChunkSetIChunkCache() {
        IChunkCache<IChunkSet> set;
        if(Objects.equals(Fawe.platform().getPlatform(), "Forge-Official")) {
            set = (x, z) -> IntSetBlocks.newInstance();
        } else {
            set = (x, z) -> CharSetBlocks.newInstance();
        }
        return set;
    }


}
