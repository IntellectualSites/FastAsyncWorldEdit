package com.sk89q.worldedit.util.command.parametric;

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
    private final Set<Character> valueFlags = new HashSet<Character>();
    private final boolean anyFlags;
    private final Set<Character> legacyFlags = new HashSet<Character>();
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

        List<String> paramNames = new ArrayList<>();
        List<String> typeStrings = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        {
            boolean checkType = false;
            for (String argument : arguments) {
                if (checkType) {
                    typeStrings.set(typeStrings.size() - 1, argument);
                } else {
                    checkType = false;
                    if (argument.equals("=")) {
                        checkType = true;
                    } else if (argument.length() == 1 && command.flags().contains(argument)) {
                        typeStrings.add("java.lang.Boolean");
                        paramNames.add(argument);
                    } else {
                        typeStrings.add("java.lang.String");
                        paramNames.add(argument);
                    }
                }
            }
            Map<Type, Binding> bindings = builder.getBindings();
            Map<String, Type> unqualified = new HashMap<>();
            for (Map.Entry<Type, Binding> entry : bindings.entrySet()) {
                Type type = entry.getKey();
                String typeStr = type.getTypeName();
                unqualified.put(typeStr, type);
                unqualified.put(typeStr.substring(typeStr.lastIndexOf('.') + 1), type);
            }
            for (String typeStr : typeStrings) {
                Type type = unqualified.get(typeStr);
                if (type == null) type = unqualified.get("java.lang.String");
                types.add(type);
            }
        }

        parameters = new ParameterData[paramNames.size()];
        List<Parameter> userParameters = new ArrayList<Parameter>();

        // This helps keep tracks of @Nullables that appear in the middle of a list
        // of parameters
        int numOptional = 0;
//
        // Go through each parameter
        for (int i = 0; i < types.size(); i++) {
            Type type = types.get(i);

            ParameterData parameter = new ParameterData();
            parameter.setType(type);
            parameter.setModifiers(new Annotation[0]);

            String paramName = paramNames.get(i);
            boolean flag = paramName.length() == 1 && command.flags().contains(paramName);
            if (flag) {
                parameter.setFlag(paramName.charAt(0), type != boolean.class && type != Boolean.class);
            }

            // TODO switch / Optional  / Search for annotations /

            parameter.setName(paramName);

            // Track all value flags
            if (parameter.isValueFlag()) {
                valueFlags.add(parameter.getFlag());
            }

            // No special @annotation binding... let's check for the type
            if (parameter.getBinding() == null) {
                parameter.setBinding(builder.getBindings().get(type));

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