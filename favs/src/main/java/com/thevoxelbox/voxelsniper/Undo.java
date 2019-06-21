/*
 This file is part of VoxelSniper, licensed under the MIT License (MIT).

 Copyright (c) The VoxelBox <http://thevoxelbox.com>
 Copyright (c) contributors

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package com.thevoxelbox.voxelsniper;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * Holds {@link BlockState}s that can be later on used to reset those block
 * locations back to the recorded states.
 */
public class Undo {

    int size;
    private World world;

    /**
     * Default constructor of a Undo container.
     */
    public Undo() {}

    /**
     * Get the number of blocks in the collection.
     *
     * @return size of the Undo collection
     */
    public int getSize() {
        return size;
    }

    /**
     * Adds a Block to the collection.
     *
     * @param block Block to be added
     */
    public void put(Block block) {
        size++;
    }


    /**
     * Set the blockstates of all recorded blocks back to the state when they
     * were inserted.
     */
    public void undo() {

    }
}
