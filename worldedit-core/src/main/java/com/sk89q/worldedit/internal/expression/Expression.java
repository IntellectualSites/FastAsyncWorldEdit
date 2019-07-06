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

package com.sk89q.worldedit.internal.expression;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.expression.lexer.Lexer;
import com.sk89q.worldedit.internal.expression.lexer.tokens.Token;
import com.sk89q.worldedit.internal.expression.parser.Parser;
import com.sk89q.worldedit.internal.expression.runtime.Constant;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.internal.expression.runtime.ExpressionEnvironment;
import com.sk89q.worldedit.internal.expression.runtime.ExpressionTimeoutException;
import com.sk89q.worldedit.internal.expression.runtime.Functions;
import com.sk89q.worldedit.internal.expression.runtime.RValue;
import com.sk89q.worldedit.internal.expression.runtime.ReturnException;
import com.sk89q.worldedit.internal.expression.runtime.Variable;
import com.sk89q.worldedit.session.request.Request;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Compiles and evaluates expressions.
 *
 * <p>Supported operators:</p>
 *
 * <ul>
 *     <li>Logical: &amp;&amp;, ||, ! (unary)</li>
 *     <li>Bitwise: ~ (unary), &gt;&gt;, &lt;&lt;</li>
 *     <li>Arithmetic: +, -, *, /, % (modulo), ^ (power), - (unary), --, ++ (prefix only)</li>
 *     <li>Comparison: &lt;=, &gt;=, &gt;, &lt;, ==, !=, ~= (near)</li>
 * </ul>
 *
 * <p>Supported functions: abs, acos, asin, atan, atan2, cbrt, ceil, cos, cosh,
 * exp, floor, ln, log, log10, max, max, min, min, rint, round, sin, sinh,
 * sqrt, tan, tanh and more. (See the Functions class or the wiki)</p>
 *
 * <p>Constants: e, pi</p>
 *
 * <p>To compile an equation, run
 * {@code Expression.compile("expression here", "var1", "var2"...)}.
 * If you wish to run the equation multiple times, you can then optimize it,
 * by calling {@link #optimize()}. You can then run the equation as many times
 * as you want by calling {@link #evaluate(double...)}. You do not need to
 * pass values for all variables specified while compiling.
 * To query variables after evaluation, you can use
 * {@link #getVariable(String, boolean)}. To get a value out of these, use
 * {@link Variable#getValue()}.</p>
 *
 * <p>Variables are also supported and can be set either by passing values
 * to {@link #evaluate(double...)}.</p>
 */
public class Expression {

    private static final ThreadLocal<ArrayDeque<Expression>> instance = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ExecutorService evalThread = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("worldedit-expression-eval-%d")
                    .build());

    private final Map<String, RValue> variables = new HashMap<>();
    private final String[] variableNames;
    private Variable[] variableArray;
    private RValue root;
    private final Functions functions = new Functions();
    private ExpressionEnvironment environment;

    public static Expression compile(String expression, String... variableNames) throws ExpressionException {
        return new Expression(expression, variableNames);
    }

    public Expression(double constant) {
        variableNames = null;
        root = new Constant(0, constant);
    }

    private Expression(String expression, String... variableNames) throws ExpressionException {
        this(Lexer.tokenize(expression), variableNames);
    }

    private Expression(List<Token> tokens, String... variableNames) throws ExpressionException {
        this.variableNames = variableNames;

        variables.put("e", new Constant(-1, Math.E));
        variables.put("pi", new Constant(-1, Math.PI));
        variables.put("true", new Constant(-1, 1));
        variables.put("false", new Constant(-1, 0));

        variableArray = new Variable[variableNames.length];
        for (int i = 0; i < variableNames.length; i++) {
            String variableName = variableNames[i];
            if (variables.containsKey(variableName)) {
                throw new ExpressionException(-1, "Tried to overwrite identifier '" + variableName + "'");
            }
            Variable var = new Variable(0);
            variables.put(variableName, var);
            variableArray[i] = var;
        }

        root = Parser.parse(tokens, this);
    }

    public double evaluate(double... values) throws EvaluationException {
        if (root instanceof Constant) {
            return root.getValue();
        }
        for (int i = 0; i < values.length; i++) {
            Variable var = variableArray[i];
            var.value = values[i];
        }
        pushInstance();
        return evaluate(values, WorldEdit.getInstance().getConfiguration().calculationTimeout);
    }

    public double evaluate(double[] values, int timeout) throws EvaluationException {
        for (int i = 0; i < values.length; ++i) {
            final String variableName = variableNames[i];
            final RValue invokable = variables.get(variableName);
            if (!(invokable instanceof Variable)) {
                throw new EvaluationException(invokable.getPosition(), "Tried to assign constant " + variableName + ".");
            }

            ((Variable) invokable).value = values[i];
        }

        try {
            if (timeout < 0) {
                return evaluateRoot();
            }
            return evaluateRootTimed(timeout);
        } catch (ReturnException e) {
            return e.getValue();
        } // other evaluation exceptions are thrown out of this method
    }

    private double evaluateRootTimed(int timeout) throws EvaluationException {
        Request request = Request.request();
        Future<Double> result = evalThread.submit(() -> {
            Request local = Request.request();
            local.setSession(request.getSession());
            local.setWorld(request.getWorld());
            local.setEditSession(request.getEditSession());
            try {
                return Expression.this.evaluateRoot();
            } finally {
                Request.reset();
            }
        });
        try {
            return result.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            result.cancel(true);
            throw new ExpressionTimeoutException("Calculations exceeded time limit.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof EvaluationException) {
                throw (EvaluationException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private Double evaluateRoot() throws EvaluationException {
        pushInstance();
        try {
            return root.getValue();
        } finally {
            popInstance();
        }
    }

    public void optimize() throws EvaluationException {
        root = root.optimize();
    }

    public RValue getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public RValue getVariable(String name, boolean create) {
        RValue variable = variables.get(name);
        if (variable == null && create) {
            variables.put(name, variable = new Variable(0));
        }

        return variable;
    }

    public static Expression getInstance() {
        return instance.get().peek();
    }

    private void pushInstance() {
        ArrayDeque<Expression> foo = instance.get();
        foo.push(this);
    }

    private void popInstance() {
        ArrayDeque<Expression> foo = instance.get();

        foo.pop();
    }

    public Functions getFunctions() {
        return functions;
    }

    public ExpressionEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(ExpressionEnvironment environment) {
        this.environment = environment;
    }

}
