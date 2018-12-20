/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.CombinedTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to 'bake' a transform into a clipboard.
 * <p>
 * <p>This class needs a better name and may need to be made more generic.</p>
 *
 * @see Clipboard
 * @see Transform
 */
public class FlattenedClipboardTransform {

    private final Clipboard original;
    private final Transform transform;

    /**
     * Create a new instance.
     *
     * @param original  the original clipboard
     * @param transform the transform
     */
    private FlattenedClipboardTransform(Clipboard original, Transform transform) {
        checkNotNull(original);
        checkNotNull(transform);
        this.original = original;
        this.transform = transform;
    }

    /**
     * Get the transformed region.
     *
     * @return the transformed region
     */
    public Region getTransformedRegion() {
        Region region = original.getRegion();
        Vector minimum = region.getMinimumPoint();
        Vector maximum = region.getMaximumPoint();

        Transform transformAround =
                new CombinedTransform(
                        new AffineTransform().translate(original.getOrigin().multiply(-1)),
                        transform,
                        new AffineTransform().translate(original.getOrigin()));

        // new Vector(minimum.getX(), minimum.getY(), minimum.getZ())
        // new Vector(maximum.getX(), maximum.getY(), maximum.getZ())
        Vector[] corners = new Vector[]{
                minimum,
                maximum,
                new Vector(maximum.getX(), minimum.getY(), minimum.getZ()),
                new Vector(minimum.getX(), maximum.getY(), minimum.getZ()),
                new Vector(minimum.getX(), minimum.getY(), maximum.getZ()),
                new Vector(minimum.getX(), maximum.getY(), maximum.getZ()),
                new Vector(maximum.getX(), minimum.getY(), maximum.getZ()),
                new Vector(maximum.getX(), maximum.getY(), minimum.getZ())};

        for (int i = 0; i < corners.length; i++) {
            corners[i] = transformAround.apply(new Vector(corners[i]));
        }

        Vector newMinimum = corners[0];
        Vector newMaximum = corners[0];

        for (int i = 1; i < corners.length; i++) {
            newMinimum = Vector.getMinimum(newMinimum, corners[i]);
            newMaximum = Vector.getMaximum(newMaximum, corners[i]);
        }

        // After transformation, the points may not really sit on a block,
        // so we should expand the region for edge cases
        newMinimum.mutX(Math.ceil(Math.floor(newMinimum.getX())));
        newMinimum.mutY(Math.ceil(Math.floor(newMinimum.getY())));
        newMinimum.mutZ(Math.ceil(Math.floor(newMinimum.getZ())));

        return new CuboidRegion(newMinimum, newMaximum);
    }

    /**
     * Create an operation to copy from the original clipboard to the given extent.
     *
     * @param target the target
     * @return the operation
     */
    public Operation copyTo(Extent target) {
        Extent extent = original;
        if (transform != null && !transform.isIdentity()) extent = new BlockTransformExtent(original, transform);
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, original.getRegion(), original.getOrigin(), target, original.getOrigin());
        copy.setTransform(transform);
        return copy;
    }

    /**
     * Create a new instance to bake the transform with.
     *
     * @param original  the original clipboard
     * @param transform the transform
     * @return a builder
     */
    public static FlattenedClipboardTransform transform(Clipboard original, Transform transform) {
        return new FlattenedClipboardTransform(original, transform);
    }


}
