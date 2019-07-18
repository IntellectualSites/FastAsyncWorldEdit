package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.image.ImageUtil;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.internal.annotation.Range;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.internal.annotation.Validate;
import com.sk89q.worldedit.util.command.parametric.ArgumentStack;
import com.sk89q.worldedit.util.command.parametric.BindingBehavior;
import com.sk89q.worldedit.util.command.parametric.BindingMatch;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.lang.annotation.Annotation;
import java.net.URI;

public class FawePrimitiveBinding {
    @BindingMatch(type = {Long.class, long.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Long getLong(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        try {
            Long v = Long.parseLong(context.next());
            validate(v, modifiers);
            return v;

        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private static void validate(long number, Annotation[] modifiers) throws ParameterException {
        for (Annotation modifier : modifiers) {
            if (modifier instanceof Range) {
                Range range = (Range) modifier;
                if (number < range.min()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is greater than or equal to %s " +
                                            "(you entered %s)", range.min(), number));
                } else if (number > range.max()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is less than or equal to %s " +
                                            "(you entered %s)", range.max(), number));
                }
            }
        }
    }

    public class ImageUri {
        public final URI uri;
        private BufferedImage image;

        ImageUri(URI uri) {
            this.uri = uri;
        }

        public BufferedImage load() throws ParameterException {
            if (image != null) {
                return image;
            }
            return image = ImageUtil.load(uri);
        }
    }

    @BindingMatch(
            type = {ImageUri.class},
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true
    )
    public ImageUri getImage(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        return new ImageUri(ImageUtil.getImageURI(context.next()));
    }

    @BindingMatch(
            type = {TextureUtil.class},
            behavior = BindingBehavior.PROVIDES
    )
    public TextureUtil getTexture(ArgumentStack context) {
        Actor actor = context.getContext().getLocals().get(Actor.class);
        if (actor == null) {
            return Fawe.get().getCachedTextureUtil(true, 0, 100);
        }
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        return session.getTextureUtil();
    }


    @BindingMatch(
            type = {Extent.class},
            behavior = BindingBehavior.PROVIDES
    )
    public Extent getExtent(ArgumentStack context) throws ParameterException {
        Extent extent = context.getContext().getLocals().get(EditSession.class);
        if (extent != null) {
            return extent;
        }
        extent = Request.request().getExtent();
        if (extent != null) {
            return extent;
        }
        Actor actor = context.getContext().getLocals().get(Actor.class);
        if (actor == null) {
            throw new ParameterException("No player to get a session for");
        }
        if (!(actor instanceof Player)) {
            throw new ParameterException("Caller is not a player");
        }
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        EditSession editSession = session.createEditSession((Player) actor);
        editSession.enableQueue();
        context.getContext().getLocals().put(EditSession.class, editSession);
        session.tellVersion(actor);
        return editSession;
    }

    /**
     * Gets an {@link com.boydti.fawe.object.FawePlayer} from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return a FawePlayer
     * @throws ParameterException on other error
     */
    @BindingMatch(type = FawePlayer.class,
                  behavior = BindingBehavior.PROVIDES)
    public FawePlayer getFawePlayer(ArgumentStack context) throws ParameterException, InputParseException {
        Actor sender = context.getContext().getLocals().get(Actor.class);
        if (sender == null) {
            throw new ParameterException("Missing 'Actor'");
        } else {
            return FawePlayer.wrap(sender);
        }
    }

    /**
     * Gets an {@link com.sk89q.worldedit.extent.Extent} from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return an extent
     * @throws ParameterException on other error
     */
    @BindingMatch(type = ResettableExtent.class,
                  behavior = BindingBehavior.PROVIDES)
    public ResettableExtent getResettableExtent(ArgumentStack context) throws ParameterException, InputParseException {
        String input = context.next();
        if (input.equalsIgnoreCase("#null")) {
            return new NullExtent();
        }
        DefaultTransformParser parser = Fawe.get().getTransformParser();
        Actor actor = context.getContext().getLocals().get(Actor.class);
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(context.getContext().getLocals().get(Actor.class));
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                parserContext.setWorld((World) extent);
            }
        }
        parserContext.setSession(WorldEdit.getInstance().getSessionManager().get(actor));
        return parser.parseFromInput(input, parserContext);
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param text the text annotation
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(classifier = Text.class,
                  type = String.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = -1,
                  provideModifiers = true)
    public String getText(ArgumentStack context, Text text, Annotation[] modifiers)
            throws ParameterException {
        String v = context.remaining();
        validate(v, modifiers);
        return v;
    }

    @BindingMatch(type = {Expression.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1)
    public Expression getExpression(ArgumentStack context) throws ParameterException, ExpressionException {
        String input = context.next();
        try {
            return new Expression(Double.parseDouble(input));
        } catch (NumberFormatException e1) {
            try {
                Expression expression = Expression.compile(input);
                expression.optimize();
                return expression;
            } catch (EvaluationException e) {
                throw new ParameterException(String.format(
                        "Expected '%s' to be a valid number (or a valid mathematical expression)", input));
            } catch (ExpressionException e) {
                throw new ParameterException(String.format(
                        "Expected '%s' to be a number or valid math expression (error: %s)", input, e.getMessage()));
            }
        }
    }


    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = String.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public String getString(ArgumentStack context, Annotation[] modifiers)
            throws ParameterException {
        String v = context.next();
        validate(v, modifiers);
        return v;
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = {Boolean.class, boolean.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1)
    public Boolean getBoolean(ArgumentStack context) throws ParameterException {
        return context.nextBoolean();
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = Vector3.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Vector3 getVector3(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        String radiusString = context.next();
        String[] radii = radiusString.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 3:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusY = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[2]));
                break;

            default:
                throw new ParameterException("You must either specify 1 or 3 radius values.");
        }
        return Vector3.at(radiusX, radiusY, radiusZ);
    }


    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = Vector2.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Vector2 getVector2(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        String radiusString = context.next();
        String[] radii = radiusString.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 2:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                break;

            default:
                throw new ParameterException("You must either specify 1 or 2 radius values.");
        }
        return Vector2.at(radiusX, radiusZ);
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = BlockVector3.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public BlockVector3 getBlockVector3(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        String radiusString = context.next();
        String[] radii = radiusString.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 3:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusY = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[2]));
                break;

            default:
                throw new ParameterException("You must either specify 1 or 3 radius values.");
        }
        return BlockVector3.at(radiusX, radiusY, radiusZ);
    }


    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = BlockVector2.class,
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public BlockVector2 getBlockVector2(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        String radiusString = context.next();
        String[] radii = radiusString.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 2:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                break;

            default:
                throw new ParameterException("You must either specify 1 or 2 radius values.");
        }
        return BlockVector2.at(radiusX, radiusZ);
    }

    /**
     * Try to parse numeric input as either a number or a mathematical expression.
     *
     * @param input input
     * @return a number
     * @throws ParameterException thrown on parse error
     */
    public static
    @Nullable
    Double parseNumericInput(@Nullable String input) throws ParameterException {
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
                throw new ParameterException(String.format(
                        "Expected '%s' to be a valid number (or a valid mathematical expression)", input));
            } catch (ExpressionException e) {
                throw new ParameterException(String.format(
                        "Expected '%s' to be a number or valid math expression (error: %s)", input, e.getMessage()));
            }
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = {Integer.class, int.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Integer getInteger(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = parseNumericInput(context.next());
        if (v != null) {
            int intValue = v.intValue();
            validate(intValue, modifiers);
            return intValue;
        } else {
            return null;
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = {Short.class, short.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Short getShort(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Integer v = getInteger(context, modifiers);
        if (v != null) {
            return v.shortValue();
        }
        return null;
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = {Double.class, double.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Double getDouble(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = parseNumericInput(context.next());
        if (v != null) {
            validate(v, modifiers);
            return v;
        } else {
            return null;
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = {Float.class, float.class},
                  behavior = BindingBehavior.CONSUMES,
                  consumedCount = 1,
                  provideModifiers = true)
    public Float getFloat(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = getDouble(context, modifiers);
        if (v != null) {
            return v.floatValue();
        }
        return null;
    }

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(double number, Annotation[] modifiers)
            throws ParameterException {
        for (Annotation modifier : modifiers) {
            if (modifier instanceof Range) {
                Range range = (Range) modifier;
                if (number < range.min()) {
                    throw new ParameterException(
                            String.format("A valid value is greater than or equal to %s (you entered %s)", range.min(), number));
                } else if (number > range.max()) {
                    throw new ParameterException(
                            String.format("A valid value is less than or equal to %s (you entered %s)", range.max(), number));
                }
            }
        }
    }

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(int number, Annotation[] modifiers)
            throws ParameterException {
        for (Annotation modifier : modifiers) {
            if (modifier instanceof Range) {
                Range range = (Range) modifier;
                if (number < range.min()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is greater than or equal to %s (you entered %s)", range.min(), number));
                } else if (number > range.max()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is less than or equal to %s (you entered %s)", range.max(), number));
                }
            }
        }
    }

    /**
     * Validate a string value using relevant modifiers.
     *
     * @param string the string
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(String string, Annotation[] modifiers)
            throws ParameterException {
        if (string == null) {
            return;
        }

        for (Annotation modifier : modifiers) {
            if (modifier instanceof Validate) {
                Validate validate = (Validate) modifier;

                if (!validate.regex().isEmpty()) {
                    if (!string.matches(validate.regex())) {
                        throw new ParameterException(
                                String.format(
                                        "The given text doesn't match the right format (technically speaking, the 'format' is %s)",
                                        validate.regex()));
                    }
                }
            }
        }
    }
}
