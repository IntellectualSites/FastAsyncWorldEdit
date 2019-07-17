package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.IOException;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mask that evaluates an expression.
 * <p>
 * <p>Expressions are evaluated as {@code true} if they return a value
 * greater than {@code 0}.</p>
 */
public class ExpressionPattern extends AbstractPattern {
    private final Expression expression;

    /**
     * Create a new instance.
     *
     * @param input the expression
     * @throws ExpressionException thrown if there is an error with the expression
     */
    public ExpressionPattern(String input) throws ExpressionException {
        checkNotNull(input);
        this.expression = Expression.compile(input, "x", "y", "z");
    }

    /**
     * Create a new instance.
     *
     * @param expression the expression
     */
    public ExpressionPattern(Expression expression) {
        checkNotNull(expression);
        this.expression = expression;
    }

    @Override
    public BaseBlock apply(BlockVector3 vector) {
        try {
            if (expression.getEnvironment() instanceof WorldEditExpressionEnvironment) {
                ((WorldEditExpressionEnvironment) expression.getEnvironment()).setCurrentBlock(vector.toVector3());
            }
            double combined = expression.evaluate(vector.getX(), vector.getY(), vector.getZ());
            return BlockState.getFromInternalId((int) combined).toBaseBlock();
        } catch (EvaluationException e) {
            e.printStackTrace();
            return BlockTypes.AIR.getDefaultState().toBaseBlock();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}
