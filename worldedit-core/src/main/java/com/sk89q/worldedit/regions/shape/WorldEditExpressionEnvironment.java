/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions.shape;

import com.fastasyncworldedit.core.math.MutableVector3;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.expression.ExpressionEnvironment;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;

public class WorldEditExpressionEnvironment implements ExpressionEnvironment {

    private static final Vector3 BLOCK_CENTER_OFFSET = Vector3.at(0.5, 0.5, 0.5);

    private final Vector3 unit;
    private final Vector3 zero2;
    //FAWE start - MutableVector3
    private Vector3 current = new MutableVector3(Vector3.ZERO);
    //FAWE end
    private final Extent extent;

    public WorldEditExpressionEnvironment(EditSession editSession, Vector3 unit, Vector3 zero) {
        this((Extent) editSession, unit, zero);
    }

    public WorldEditExpressionEnvironment(Extent extent, Vector3 unit, Vector3 zero) {
        this.extent = extent;
        this.unit = unit;
        this.zero2 = zero.add(BLOCK_CENTER_OFFSET);
    }

    public BlockVector3 toWorld(double x, double y, double z) {
        // unscale, unoffset, round-nearest
        return Vector3.at(x, y, z).multiply(unit).add(zero2).toBlockPoint();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockType(double x, double y, double z) {
        return extent.getBlock(toWorld(x, y, z)).getBlockType().getLegacyCombinedId() >> 4;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockData(double x, double y, double z) {
        return extent.getBlock(toWorld(x, y, z)).getBlockType().getLegacyCombinedId() & 0xF;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockTypeAbs(double x, double y, double z) {
        return extent.getBlock(BlockVector3.at(x, y, z)).getBlockType().getLegacyCombinedId() >> 4;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockDataAbs(double x, double y, double z) {
        return extent.getBlock(toWorld(x, y, z)).getBlockType().getLegacyCombinedId() & 0xF;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBlockTypeRel(double x, double y, double z) {
        return extent.getBlock(toWorldRel(x, y, z).toBlockPoint()).getBlockType().getLegacyCombinedId() >> 4;
    }

    @SuppressWarnings("deprecation")
    public int getBlockDataRel(double x, double y, double z) {
        return extent.getBlock(toWorldRel(x, y, z).toBlockPoint()).getBlockType().getLegacyCombinedId() & 0xF;
    }

    //FAWE start
    public void setCurrentBlock(int x, int y, int z) {
        current.setComponents(x, y, z);
    }

    public Vector3 toWorldRel(double x, double y, double z) {
        return current.add(x, y, z);
    }

    public WorldEditExpressionEnvironment clone() {
        return new WorldEditExpressionEnvironment(extent, unit, zero2.subtract(BLOCK_CENTER_OFFSET));
    }
    //FAWe end

    public void setCurrentBlock(Vector3 current) {
        this.current = current;
    }
}
