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

package com.fastasyncworldedit.core.math;

import com.fastasyncworldedit.core.util.collection.BlockVector3Set;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalBlockVectorSetTest {

    @Test
    void upgradesWhenBlockVectorIsOutsideLocalRange() {
        BlockVector3Set set = LocalBlockVectorSet.wrapped();
        BlockVector3 position = BlockVector3.at(-38, -357, -20);

        assertTrue(set.add(position));
        assertTrue(set.contains(position));
    }

    @Test
    void retainsExistingVectorsWhenUpgraded() {
        BlockVector3Set set = LocalBlockVectorSet.wrapped();
        BlockVector3 originalPosition = BlockVector3.at(-38, 128, -20);
        BlockVector3 positionOutsideLocalRange = BlockVector3.at(-38, -357, -20);

        assertTrue(set.add(originalPosition));
        assertTrue(set.add(positionOutsideLocalRange));
        assertTrue(set.contains(originalPosition));
        assertTrue(set.contains(positionOutsideLocalRange));
    }

    @Test
    void upgradesWhenBulkAddingVectorsOutsideLocalRange() {
        BlockVector3Set set = LocalBlockVectorSet.wrapped();
        BlockVector3 positionInsideLocalRange = BlockVector3.at(-38, 128, -20);
        BlockVector3 positionOutsideLocalRange = BlockVector3.at(-38, -357, -20);

        assertTrue(set.addAll(List.of(positionInsideLocalRange, positionOutsideLocalRange)));
        assertTrue(set.contains(positionInsideLocalRange));
        assertTrue(set.contains(positionOutsideLocalRange));
    }

}
