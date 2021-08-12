package com.fastasyncworldedit.core.extension.factory.parser.pattern;

import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.function.pattern.ExpressionPattern;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.registry.SimpleInputParser;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.Collections;
import java.util.List;

public class ExpressionPatternParser extends SimpleInputParser<Pattern> {

    private final List<String> aliases = Collections.singletonList("=");

    /**
     * Create a new simple parser with a defined prefix for the result.
     *
     * @param worldEdit the worldedit instance.
     */
    public ExpressionPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public List<String> getMatchedAliases() {
        return this.aliases;
    }

    @Override
    public Pattern parseFromSimpleInput(String input, ParserContext context) throws InputParseException {
        try {
            Expression exp = Expression.compile(input.substring(1), "x", "y", "z");
            WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                    context.requireExtent(), Vector3.ONE, Vector3.ZERO);
            exp.setEnvironment(env);
            return new ExpressionPattern(exp);
        } catch (ExpressionException e) {
            throw new InputParseException(Caption.of(
                    "worldedit.error.parser.invalid-expression",
                    TextComponent.of(e.getMessage())
            ));
        }
    }

}
