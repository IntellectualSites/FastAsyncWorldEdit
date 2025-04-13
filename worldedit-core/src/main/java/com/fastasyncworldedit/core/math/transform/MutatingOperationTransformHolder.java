package com.fastasyncworldedit.core.math.transform;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;

import java.util.function.Function;

/**
 * Applies an operation on a {@link Transform} when {@link MutatingOperationTransformHolder#mutate()} is called. Typically at
 * the start of an operation.
 *
 * @param <T> transform to mutate type
 * @since TODO
 */
public class MutatingOperationTransformHolder<T extends Transform> implements Transform {

    private final Function<? super T, ? extends T> operation;
    private T transform;

    /**
     * Construct new instance
     *
     * @param transform transform to mutate
     * @since TODO
     */
    public MutatingOperationTransformHolder(T transform, Function<? super T, ? extends T> operation) {
        this.transform = transform;
        this.operation = operation;
    }

    /**
     * Apply the mutator to the transform
     *
     * @since TODO
     */
    public void mutate() {
        if (operation != null) {
            transform = operation.apply(transform);
        }
    }

    @Override
    public boolean isIdentity() {
        return transform.isIdentity();
    }

    @Override
    public Vector3 apply(final Vector3 input) {
        return transform.apply(input);
    }

    @Override
    public Transform inverse() {
        return transform.inverse();
    }

    @Override
    public Transform combine(final Transform other) {
        return transform.combine(other);
    }

}
