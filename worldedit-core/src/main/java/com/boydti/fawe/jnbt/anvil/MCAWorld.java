package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.beta.IChunkGet;
import com.boydti.fawe.beta.implementation.packet.ChunkPacket;
import com.google.common.base.Preconditions;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.block.BlockState;

import javax.annotation.Nullable;
import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;

public class MCAWorld extends AbstractWorld {
    private final File path;

    public MCAWorld(File path) {
        checkArgument(path.isDirectory());
        this.path = path;
    }

    @Override
    public String getName() {
        return path.getName();
    }


    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tile) throws WorldEditException {
        return false;
    }

    @Override
    public boolean notifyAndLightBlock(BlockVector3 position, BlockState previousType) throws WorldEditException {
        return false;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return false;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {

    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {

    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        return false;
    }

    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return null;
    }

    @Override
    public void refreshChunk(int chunkX, int chunkZ) {

    }

    @Override
    public IChunkGet get(int x, int z) {
        return null;
    }

    @Override
    public void sendFakeChunk(@Nullable Player player, ChunkPacket packet) {

    }
}
