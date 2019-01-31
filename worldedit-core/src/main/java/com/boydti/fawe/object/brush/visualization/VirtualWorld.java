package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.BlockInteractEvent;
import com.sk89q.worldedit.event.platform.PlayerInputEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.SimpleWorld;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.io.Closeable;
import java.io.IOException;

public interface VirtualWorld extends SimpleWorld, FaweQueue, Closeable {
	Vector3 getOrigin();

    FaweChunk getSnapshot(int chunkX, int chunkZ);

    @Override
    default BaseBlock getFullBlock(BlockVector3 position) {
        return getLazyBlock(position).toBaseBlock();
    }

    @Override
    int getMaxY();

    @Override
    boolean setBlock(BlockVector3 pt, BlockStateHolder block) throws WorldEditException;

    @Override
    default BlockVector3 getMaximumPoint() {
        return FaweQueue.super.getMaximumPoint();
    }

    @Override
    default BlockVector3 getMinimumPoint() {
        return FaweQueue.super.getMinimumPoint();
    }

    FawePlayer getPlayer();

    void update();

    @Override
    default void close() throws IOException {
        close(true);
    }

    void close(boolean update) throws IOException;

    default void handleBlockInteract(Player player, BlockVector3 pos, BlockInteractEvent event) {}

    default void handlePlayerInput(Player player, PlayerInputEvent event) {}
}
