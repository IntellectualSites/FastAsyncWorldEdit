package com.sk89q.worldedit.extension.platform.binding;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.internal.expression.EvaluationException;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import java.util.Locale;
import javax.annotation.Nullable;

public class PrimitiveBindings extends Bindings {
    public PrimitiveBindings(WorldEdit worldEdit) {
        super(worldEdit);
    }

    /*
    Parsers
     */
    @Binding
    public Expression getExpression(String argument) throws ExpressionException {
        try {
            Expression expression = Expression.compile(argument);
            expression.optimize();
            return expression;
        } catch (EvaluationException e) {
            throw new InputParseException(String.format(
                    "Expected '%s' to be a valid number (or a valid mathematical expression)", argument));
        } catch (ExpressionException e) {
            throw new InputParseException(String.format(
                    "Expected '%s' to be a number or valid math expression (error: %s)", argument, e.getMessage()));
        }
    }

// TODO: Ping @MattBDev to reimplement 2020-02-04
//
//    /**
//     * Gets an {@link Extent} from a {@link Binding}.
//     *
//     * @param argument the context
//     * @return an extent
//     * @throws InputParseException on other error
//     */
//    @Binding
//    public ResettableExtent getResettableExtent(Actor actor, String argument) throws InputParseException {
//        if (argument.equalsIgnoreCase("#null")) {
//            return new NullExtent();
//        }
//        DefaultTransformParser parser = Fawe.get().getTransformParser();
//        ParserContext parserContext = new ParserContext();
//        if (actor instanceof Entity) {
//            Extent extent = ((Entity) actor).getExtent();
//            if (extent instanceof World) {
//                parserContext.setWorld((World) extent);
//            }
//        }
//        parserContext.setSession(WorldEdit.getInstance().getSessionManager().get(actor));
//        return parser.parseFromInput(argument, parserContext);
//    }

    /**
     * Gets a type from a {@link Binding}.
     *
     * @param argument the context
     * @return the requested type
     * @throws InputParseException on error
     */
    @Binding
    public Boolean getBoolean(String argument) {
        switch (argument.toLowerCase(Locale.ROOT)) {
            case "":
                return null;
            case "true":
            case "yes":
            case "on":
            case "y":
            case "1":
            case "t":
                return true;
            case "false":
            case "no":
            case "off":
            case "f":
            case "n":
            case "0":
                return false;
            default:
                throw new InputParseException("Invalid boolean " + argument);
        }
    }

    /**
     * Gets a type from a {@link Binding}.
     *
     * @param argument the context
     * @return the requested type
     * @throws InputParseException on error
     */
    @Binding
    public Vector3 getVector3(String argument) {
        String[] radii = argument.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = PrimitiveBindings.parseNumericInput(radii[0]);
                break;

            case 3:
                radiusX = PrimitiveBindings.parseNumericInput(radii[0]);
                radiusY = PrimitiveBindings.parseNumericInput(radii[1]);
                radiusZ = PrimitiveBindings.parseNumericInput(radii[2]);
                break;

            default:
                throw new InputParseException("You must either specify 1 or 3 radius values.");
        }
        return Vector3.at(radiusX, radiusY, radiusZ);
    }


    /**
     * Gets a type from a {@link Binding}.
     *
     * @param argument the context
     * @return the requested type
     * @throws InputParseException on error
     */
    @Binding
    public Vector2 getVector2(String argument) {
        String radiusString = argument;
        String[] radii = radiusString.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = PrimitiveBindings.parseNumericInput(radii[0]);
                break;

            case 2:
                radiusX = PrimitiveBindings.parseNumericInput(radii[0]);
                radiusZ = PrimitiveBindings.parseNumericInput(radii[1]);
                break;

            default:
                throw new InputParseException("You must either specify 1 or 2 radius values.");
        }
        return Vector2.at(radiusX, radiusZ);
    }

    /**
     * Gets a type from a {@link Binding}.
     *
     * @param argument the context
     * @return the requested type
     * @throws InputParseException on error
     */
    @Binding
    public BlockVector3 getBlockVector3(String argument) {
        String radiusString = argument;
        String[] radii = radiusString.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = PrimitiveBindings.parseNumericInput(radii[0]);
                break;

            case 3:
                radiusX = PrimitiveBindings.parseNumericInput(radii[0]);
                radiusY = PrimitiveBindings.parseNumericInput(radii[1]);
                radiusZ = PrimitiveBindings.parseNumericInput(radii[2]);
                break;

            default:
                throw new InputParseException("You must either specify 1 or 3 radius values.");
        }
        return BlockVector3.at(radiusX, radiusY, radiusZ);
    }


    /**
     * Gets a type from a {@link Binding}.
     *
     * @param argument the context
     * @return the requested type
     * @throws InputParseException on error
     */
    @Binding
    public BlockVector2 getBlockVector2(String argument) {
        String[] radii = argument.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = parseNumericInput(radii[0]);
                break;

            case 2:
                radiusX = parseNumericInput(radii[0]);
                radiusZ = parseNumericInput(radii[1]);
                break;

            default:
                throw new InputParseException("You must either specify 1 or 2 radius values.");
        }
        return BlockVector2.at(radiusX, radiusZ);
    }

    /**
     * Try to parse numeric input as either a number or a mathematical expression.
     *
     * @param input input
     * @return a number
     * @throws InputParseException thrown on parse error
     */
    public static @Nullable Double parseNumericInput(@Nullable String input) {
        if (input == null) {
            return null;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e1) {
            try {
                Expression expression = Expression.compile(input);
                return expression.evaluate();
            } catch (EvaluationException e) {
                throw new InputParseException(String.format(
                        "Expected '%s' to be a valid number (or a valid mathematical expression)", input));
            } catch (ExpressionException e) {
                throw new InputParseException(String.format(
                        "Expected '%s' to be a number or valid math expression (error: %s)", input, e.getMessage()));
            }
        }
    }
}
