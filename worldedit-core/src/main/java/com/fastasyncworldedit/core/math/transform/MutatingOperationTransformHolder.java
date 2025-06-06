package com.fastasyncworldedit.core.math.transform;

import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.CombinedTransform;
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
     * Perform a mutation on the given transform, if supported. Else, does nothing. This mutation will depend on the specific
     * transform implementation.
     * <p>
     * Implementation detail: it may be possible for this method to be called multiple times before an operation actually occurs.
     *
     * @param transform Transform to transform
     * @since TODO
     */
    public static Transform transform(Transform transform) {
        return transform(transform, false);
    }

    /**
     * Perform a mutation on the given transform, if supported. Else, does nothing. This mutation will depend on the specific
     * transform implementation.
     * <p>
     * Implementation detail: it may be possible for this method to be called multiple times before an operation actually occurs.
     *
     * @param transform Transform to transform
     * @param parallel  If the context is potentially parallel, meaning the given transform will be copied if required
     * @since TODO
     */
    public static Transform transform(Transform transform, boolean parallel) {
        return switch (transform) {
            case MutatingOperationTransformHolder<?> mutating -> parallel ? mutating.copy().mutate() : mutating.mutate();
            case CombinedTransform combined -> {
                if (!parallel) {
                    combined.getTransforms().forEach(t -> transform(t, false));
                    yield combined;
                }
                yield new CombinedTransform(combined.getTransforms().stream().map(t -> transform(t, true)).toList());
            }
            default -> transform;
        };
    }

    /**
     * Apply the mutator to the transform
     *
     * @since TODO
     */
    public MutatingOperationTransformHolder<T> mutate() {
        if (operation != null) {
            transform = operation.apply(transform);
        }
        return this;
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

    public MutatingOperationTransformHolder<T> copy() {
        // I don't think we should see nested mutating, but you never know
        T transform = this.transform instanceof MutatingOperationTransformHolder<?> mutating ? (T) mutating.copy() :
                this.transform;
        return new MutatingOperationTransformHolder<>(transform, operation);
    }

}
