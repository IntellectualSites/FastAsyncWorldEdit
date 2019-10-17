package com.sk89q.worldedit.command.argument;

import com.google.common.collect.ImmutableSetMultimap;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.MultiKeyConverter;
import org.enginehub.piston.converter.SimpleArgumentConverter;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

public class ExpressionConverter implements ArgumentConverter<Expression> {

    public static void register(CommandManager commandManager) {
        commandManager.registerConverter(Key.of(Expression.class), new ExpressionConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("TODO");
    }

    @Override
    public ConversionResult<Expression> convert(String s, InjectedValueAccess injectedValueAccess) {
        Expression expression;
        try {
            expression = new Expression(Double.parseDouble(s));
        } catch (NumberFormatException e1) {
            try {
                expression = Expression.compile(s);
                expression.optimize();
                return SuccessfulConversion.fromSingle(expression);
            } catch (Exception e) {
                return FailedConversion.from(e);
            }
        }
        return SuccessfulConversion.fromSingle(expression);
    }
}
