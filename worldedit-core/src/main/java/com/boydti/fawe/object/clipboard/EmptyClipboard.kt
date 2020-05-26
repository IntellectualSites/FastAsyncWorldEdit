package com.boydti.fawe.`object`.clipboard

import com.sk89q.jnbt.CompoundTag
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.entity.Entity
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector2
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.biome.BiomeType
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes

object EmptyClipboard : Clipboard {
    override fun getRegion(): Region {
        return CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO)
    }

    override fun getDimensions(): BlockVector3 {
        return BlockVector3.ZERO
    }

    override fun getOrigin(): BlockVector3 {
        return BlockVector3.ZERO
    }

    override fun setOrigin(origin: BlockVector3) {}
    override fun removeEntity(entity: Entity) {}
    override fun getMinimumPoint(): BlockVector3 {
        return BlockVector3.ZERO
    }

    override fun getMaximumPoint(): BlockVector3 {
        return BlockVector3.ZERO
    }

    override fun getFullBlock(position: BlockVector3): BaseBlock {
        return BlockTypes.AIR!!.defaultState.toBaseBlock()
    }

    override fun getBlock(position: BlockVector3): BlockState {
        return BlockTypes.AIR!!.defaultState
    }

    override fun getBiome(position: BlockVector2): BiomeType? {
        return null
    }

    @Throws(WorldEditException::class)
    override fun <T : BlockStateHolder<T>?> setBlock(position: BlockVector3, block: T): Boolean {
        return false
    }

    @Throws(WorldEditException::class)
    override fun <T : BlockStateHolder<T>?> setBlock(x: Int, y: Int, z: Int, block: T): Boolean {
        return false
    }

    @Throws(WorldEditException::class)
    override fun setTile(x: Int, y: Int, z: Int, tile: CompoundTag): Boolean {
        return false
    }

    override fun setBiome(position: BlockVector2, biome: BiomeType): Boolean {
        return false
    }

    override fun setBiome(x: Int, y: Int, z: Int, biome: BiomeType): Boolean {
        return false
    }
}
