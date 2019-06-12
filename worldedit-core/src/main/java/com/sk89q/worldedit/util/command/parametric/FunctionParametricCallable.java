package com.sk89q.worldedit.util.command.parametric;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.google.common.primitives.Chars;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.util.command.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class FunctionParametricCallable extends AParametricCallable {

    private final ParametricBuilder builder;
    private final ParameterData[] parameters;
    private final Set<Character> valueFlags = new HashSet<>();
    private final boolean anyFlags;
    private final Set<Character> legacyFlags = new HashSet<>();
    private final SimpleDescription description = new SimpleDescription();
    private final String permission;
    private final Command command;
    private final Function<Object[], ?> function;
    private final String group;

    public FunctionParametricCallable(ParametricBuilder builder, String group, Command command, String permission, List<String> arguments, Function<Object[], ?> function) {
        this.command = command;
        this.permission = permission;
        this.builder = builder;
        this.function = function;
        this.group = group;

        List<Object[]> paramParsables = new ArrayList<>();
        {
            Map<String, Type> unqualified = new HashMap<>();
            for (Type type : builder.getBindings().getTypes()) {
                String typeStr = type.getTypeName();
                unqualified.put(typeStr, type);
                unqualified.put(typeStr.substring(typeStr.lastIndexOf('.') + 1), type);
            }
            {
                Object[] param = null; // name | type | optional value
                boolean checkEq = false;
                int checkEqI = 0;
                for (String arg : arguments) {
                    if (arg.equals("=")) {
                        checkEqI++;
                        checkEq = true;
                    } else if (param == null || !checkEq) {
                        if (param != null) paramParsables.add(param);
                        param = new Object[3];
                        param[0] = arg;
                        if (arg.length() == 1 && command.flags().contains(arg)) {
                            param[1] = Boolean.class;
                        } else {
                            param[1] = String.class;
                        }
                        param[2] = null;
                        checkEqI = 0;
                        checkEq = false;
                    } else {
                        if (checkEqI == 1) {
                            param[1] = unqualified.getOrDefault(arg, String.class);
                            checkEq = false;
                        } else if (checkEqI == 2) {
                            char c = arg.charAt(0);
                            if (c == '\'' || c == '"') arg = arg.substring(1, arg.length() - 1);
                            param[2] = arg;
                            checkEqI = 0;
                            checkEq = false;
                        }
                    }
                }
                if (param != null) paramParsables.add(param);
            }
        }

        parameters = new ParameterData[paramParsables.size()];
        List<Parameter> userParameters = new ArrayList<>();

        // This helps keep tracks of @Nullables that appear in the middle of a list
        // of parameters
        int numOptional = 0;
//
        // Go through each parameter
        for (int i = 0; i < paramParsables.size(); i++) {
            Object[] parsable = paramParsables.get(i);
            String paramName = (String) parsable[0];
            Type type = (Type) parsable[1];
            String optional = (String) parsable[2];

            ParameterData parameter = new ParameterData();
            parameter.setType(type);
            parameter.setModifiers(new Annotation[0]);

            boolean flag = paramName.length() == 1 && command.flags().contains(paramName);
            if (flag) {
                parameter.setFlag(paramName.charAt(0), type != boolean.class && type != Boolean.class);
            }

            if (optional != null) {
                parameter.setOptional(true);
                if (!optional.equalsIgnoreCase("null")) parameter.setDefaultValue(new String[]{optional});
            }

            parameter.setName(paramName);

            // Track all value flags
            if (parameter.isValueFlag()) {
                valueFlags.add(parameter.getFlag());
            }

            // No special @annotation binding... let's check for the type
            if (parameter.getBinding() == null) {
                parameter.setBinding(builder.getBindings());

                // Don't know how to parse for this type of value
                if (parameter.getBinding() == null) {
                    throw new ParametricException("Don't know how to handle the parameter type '" + type + "' in\n" + StringMan.getString(command.aliases()));
                }
            }

            // Do some validation of this parameter
            parameter.validate(() -> StringMan.getString(command.aliases()), i + 1);

            // Keep track of optional parameters
            if (parameter.isOptional() && parameter.getFlag() == null) {
                numOptional++;
            } else {
                if (numOptional > 0 && parameter.isNonFlagConsumer()) {
                    if (parameter.getConsumedCount() < 0) {
                        throw new ParametricException(
                                "Found an parameter using the binding " +
                                        parameter.getBinding().getClass().getCanonicalName() +
                                        "\nthat does not know how many arguments it consumes, but " +
                                        "it follows an optional parameter\nMethod: " + StringMan.getString(command.aliases()));
                    }
                }
            }

            parameters[i] = parameter;

            // Make a list of "real" parameters
            if (parameter.isUserInput()) {
                userParameters.add(parameter);
            }
        }


        // Gather legacy flags
        anyFlags = command.anyFlags();
        legacyFlags.addAll(Chars.asList(command.flags().toCharArray()));

        // Finish description
        description.setDescription(!command.desc().isEmpty() ? command.desc() : null);
        description.setHelp(!command.help().isEmpty() ? command.help() : null);
        description.overrideUsage(!command.usage().isEmpty() ? command.usage() : null);
        description.setPermissions(Arrays.asList(permission));

        if (command.usage().isEmpty() && (command.min() > 0 || command.max() > 0)) {
            boolean hasUserParameters = false;

            for (ParameterData parameter : parameters) {
                if (parameter.getBinding().getBehavior(parameter) != BindingBehavior.PROVIDES) {
                    hasUserParameters = true;
                    break;
                }
            }

            if (!hasUserParameters) {
                description.overrideUsage("(unknown usage information)");
            }
        }

        // Set parameters
        description.setParameters(userParameters);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public Command getCommand() {
        return command;
    }

    @Override
    public ParameterData[] getParameters() {
        return parameters;
    }

    public Set<Character> getValueFlags() {
        return valueFlags;
    }

    @Override
    public Set<Character> getLegacyFlags() {
        return legacyFlags;
    }

    @Override
    public Object call(String stringArguments, CommandLocals locals, String[] parentCommands) throws CommandException {
        // Test permission
        if (!testPermission(locals)) {
            throw new CommandPermissionsException();
        }
        locals.putIfAbsent(CommandCallable.class, this);

        String calledCommand = parentCommands.length > 0 ? parentCommands[parentCommands.length - 1] : "_";
        String[] split = (calledCommand + " " + stringArguments).split(" ", -1);
        CommandContext context = new CommandContext(split, getValueFlags(), false, locals);

        // Provide help if -? is specified
        if (context.hasFlag('?')) {
            throw new InvalidUsageException(null, this, true);
        }

        Object[] args = new Object[parameters.length];
        ContextArgumentStack arguments = new ContextArgumentStack(context);
        ParameterData parameter = null;

        try {
            // preProcess handlers
            {

            }

            // Collect parameters
            for (int i = 0; i < parameters.length; i++) {
                parameter = parameters[i];

                if (mayConsumeArguments(i, arguments)) {
                    // Parse the user input into a method argument
                    ArgumentStack usedArguments = getScopedContext(parameter, arguments);

                    try {
                        usedArguments.mark();
                        args[i] = parameter.getBinding().bind(parameter, usedArguments, false);
                    } catch (ParameterException e) {
                        // Not optional? Then we can't execute this command
                        if (!parameter.isOptional()) {
                            throw e;
                        }

                        usedArguments.reset();
                        args[i] = getDefaultValue(i, arguments);
                    }
                } else {
                    args[i] = getDefaultValue(i, arguments);
                }
            }

            // Check for unused arguments
            checkUnconsumed(arguments);

            // preInvoke handlers
            {
                if (context.argsLength() < command.min()) {
                    throw new MissingParameterException();
                }
                if (command.max() != -1 && context.argsLength() > command.max()) {
                    throw new UnconsumedParameterException(context.getRemainingString(command.max()));
                }
            }

            // Execute!
            Object result = function.apply(args);

            // postInvoke handlers
            {
            }
            return result;
        } catch (MissingParameterException e) {
            throw new InvalidUsageException("Too few parameters!", this, true);
        } catch (UnconsumedParameterException e) {
            throw new InvalidUsageException("Too many parameters! Unused parameters: " + e.getUnconsumed(), this, true);
        } catch (ParameterException e) {
            assert parameter != null;
            String name = parameter.getName();

            throw new InvalidUsageException("For parameter '" + name + "': " + e.getMessage(), this, true);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof CommandException) {
                throw (CommandException) e.getCause();
            }
            throw new WrappedCommandException(e);
        } catch (Throwable t) {
            throw new WrappedCommandException(t);
        }
    }

    @Override
    public boolean testPermission(CommandLocals locals) {
        return permission != null ? (builder.getAuthorizer().testPermission(locals, permission)) : true;
    }

    @Override
    public SimpleDescription getDescription() {
        return description;
    }

    @Override
    public String[] getPermissions() {
        return new String[]{permission};
    }

    @Override
    public ParametricBuilder getBuilder() {
        return builder;
    }

    @Override
    public boolean anyFlags() {
        return anyFlags;
    }

    @Override
    public String toString() {
        return command.aliases()[0];
    }
}
