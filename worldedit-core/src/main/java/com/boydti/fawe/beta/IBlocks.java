package com.boydti.fawe.beta;

/**
 * Shared interface for IGetBlocks and ISetBlocks
 */
public interface IBlocks extends Trimable {
    boolean hasSection(int layer);

    IChunkSet reset();
}