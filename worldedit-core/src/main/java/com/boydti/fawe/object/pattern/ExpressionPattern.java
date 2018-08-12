package com.boydti.fawe.object.pattern;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
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

    public String input;
    private transient Expression expression;

    /**
     * Create a new instance.
     *
     * @param input the expression
     * @throws ExpressionException thrown if there is an error with the expression
     */
    public ExpressionPattern(String input) throws ExpressionException {
        checkNotNull(input);
        this.input = input;
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
    public BlockStateHolder apply(Vector vector) {
        try {
            if (expression.getEnvironment() instanceof WorldEditExpressionEnvironment) {
                ((WorldEditExpressionEnvironment) expression.getEnvironment()).setCurrentBlock(vector);
            }
            double combined = expression.evaluate(vector.getX(), vector.getY(), vector.getZ());
            return BlockState.get((int) combined);
        } catch (EvaluationException e) {
            e.printStackTrace();
            return EditSession.nullBlock;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            this.expression = Expression.compile(input, "x", "y", "z");
        } catch (ExpressionException e) {
            e.printStackTrace();
        }
    }
}