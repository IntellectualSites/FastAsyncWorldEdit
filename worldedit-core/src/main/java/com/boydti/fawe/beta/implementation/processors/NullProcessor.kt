package com.boydti.fawe.beta.implementation.processors

import com.boydti.fawe.beta.IBatchProcessor
import com.boydti.fawe.beta.IChunk
import com.boydti.fawe.beta.IChunkGet
import com.boydti.fawe.beta.IChunkSet
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.extent.NullExtent

object NullProcessor : IBatchProcessor {

    override fun processSet(chunk: IChunk, get: IChunkGet, set: IChunkSet): IChunkSet? {
        return null
    }

    override fun construct(child: Extent): Extent {
        return NullExtent()
    }
}
