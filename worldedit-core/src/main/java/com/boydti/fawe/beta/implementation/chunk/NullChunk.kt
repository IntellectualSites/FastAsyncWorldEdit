package com.boydti.fawe.beta.implementation.chunk

import com.boydti.fawe.beta.Filter
import com.boydti.fawe.beta.IChunkSet
import com.boydti.fawe.beta.IQueueChunk
import com.boydti.fawe.beta.implementation.filter.block.ChunkFilterBlock
import com.sk89q.jnbt.CompoundTag
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.biome.BiomeType
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import java.util.UUID
import java.util.concurrent.Future

object NullChunk : IQueueChunk<Nothing> {

    override fun getX(): Int {
        return 0
    }

    override fun getZ(): Int {
        return 0
    }

    override fun isEmpty(): Boolean {
        return true
    }

    override fun call(): Nothing? {
        return null
    }

    override fun filterBlocks(filter: Filter, block: ChunkFilterBlock, region: Region?, full: Boolean) {

    }

    //    @Override
    //    public void flood(Flood flood, FilterBlockMask mask, ChunkFilterBlock block) {
    //
    //    }

    override fun setBiome(x: Int, y: Int, z: Int, biome: BiomeType): Boolean {
        return false
    }

    override fun setTile(x: Int, y: Int, z: Int, tag: CompoundTag): Boolean {
        return false
    }

    override fun setEntity(tag: CompoundTag) {

    }

    override fun removeEntity(uuid: UUID) {

    }

    override fun getEntityRemoves(): Set<UUID>? {
        return null
    }

    override fun getBiomes(): Array<BiomeType>? {
        return null
    }

    override fun <B : BlockStateHolder<B>> setBlock(x: Int, y: Int, z: Int, block: B): Boolean {
        return false
    }

    override fun setBlocks(layer: Int, data: CharArray) {

    }

    override fun getBiomeType(x: Int, y: Int, z: Int): BiomeType? {
        return null
    }

    override fun hasSection(layer: Int): Boolean {
        return false
    }

    override fun getBlock(x: Int, y: Int, z: Int): BlockState {
        return BlockTypes.__RESERVED__!!.defaultState
    }

    override fun getFullBlock(x: Int, y: Int, z: Int): BaseBlock {
        return BlockTypes.__RESERVED__!!.defaultState.toBaseBlock()
    }

    override fun getTiles(): Map<BlockVector3, CompoundTag> {
        return emptyMap()
    }

    override fun getTile(x: Int, y: Int, z: Int): CompoundTag? {
        return null
    }

    override fun getEntities(): Set<CompoundTag> {
        return emptySet()
    }

    override fun load(layer: Int): CharArray? {
        return null
    }

    override fun getEntity(uuid: UUID): CompoundTag? {
        return null
    }

    override fun <T : Future<T>?> call(set: IChunkSet?, finalize: Runnable?): T? {
        return null
    }

    override fun trim(aggressive: Boolean): Boolean {
        return true
    }

    override fun trim(aggressive: Boolean, layer: Int): Boolean {
        return true
    }
}

