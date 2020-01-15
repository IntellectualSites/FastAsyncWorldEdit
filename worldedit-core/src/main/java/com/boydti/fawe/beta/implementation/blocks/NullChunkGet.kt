package com.boydti.fawe.beta.implementation.blocks

import com.boydti.fawe.FaweCache
import com.boydti.fawe.beta.IBlocks
import com.boydti.fawe.beta.IChunkGet
import com.boydti.fawe.beta.IChunkSet
import com.sk89q.jnbt.CompoundTag
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.biome.BiomeType
import com.sk89q.worldedit.world.biome.BiomeTypes
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockTypes

import java.util.Collections
import java.util.UUID
import java.util.concurrent.Future

object NullChunkGet : IChunkGet {

    override fun getFullBlock(x: Int, y: Int, z: Int): BaseBlock {
        return BlockTypes.AIR!!.defaultState.toBaseBlock()
    }

    override fun getBiomeType(x: Int, y: Int, z: Int): BiomeType? {
        return BiomeTypes.FOREST
    }

    override fun getBlock(x: Int, y: Int, z: Int): BlockState {
        return BlockTypes.AIR!!.defaultState
    }

    override fun getTiles(): Map<BlockVector3, CompoundTag> {
        return emptyMap()
    }

    override fun getTile(x: Int, y: Int, z: Int): CompoundTag? {
        return null
    }

    override fun getEntities(): Set<CompoundTag>? {
        return null
    }

    override fun getEntity(uuid: UUID): CompoundTag? {
        return null
    }

    override fun trim(aggressive: Boolean): Boolean {
        return true
    }

    override fun <T : Future<T>> call(set: IChunkSet, finalize: Runnable): T? {
        return null
    }

    override fun load(layer: Int): CharArray {
        return FaweCache.IMP.EMPTY_CHAR_4096
    }

    override fun hasSection(layer: Int): Boolean {
        return false
    }

    override fun reset(): IBlocks? {
        return null
    }
}
