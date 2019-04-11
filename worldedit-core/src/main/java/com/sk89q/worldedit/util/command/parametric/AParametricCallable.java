package com.sk89q.worldedit.util.command.parametric;

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.util.command.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class AParametricCallable implements CommandCallable {
//    private final ParametricBuilder builder;
//    private ParameterData[] parameters;
//    private Set<Character> valueFlags = new HashSet<Character>();
//    private boolean anyFlags;
//    private Set<Character> legacyFlags = new HashSet<Character>();
//    private SimpleDescription description = new SimpleDescription();
//    private String permission;
//    private Command command;

    public abstract ParameterData[] getParameters();
    public abstract Set<Character> getValueFlags();
    public abstract Set<Character> getLegacyFlags();
    public abstract SimpleDescription getDescription();
    public abstract String[] getPermissions();
    public abstract ParametricBuilder getBuilder();
    public abstract boolean anyFlags();
    public abstract Command getCommand();
    public Command getDefinition() {
        return getCommand();
    }
    public abstract String getGroup();
    @Override
    public abstract String toString();

    /**
     * Get the right {@link ArgumentStack}.
     *
     * @param parameter the parameter
     * @param existing  the existing scoped context
     * @return the context to use
     */
    public static ArgumentStack getScopedContext(Parameter parameter, ArgumentStack existing) {
        if (parameter.getFlag() != null) {
            CommandContext context = existing.getContext();

            if (parameter.isValueFlag()) {
                return new StringArgumentStack(context, context.getFlag(parameter.getFlag()), false);
            } else {
                String v = context.hasFlag(parameter.getFlag()) ? "true" : "false";
                return new StringArgumentStack(context, v, true);
            }
        }

        return existing;
    }

    /**
     * Get whether a parameter is allowed to consume arguments.
     *
     * @param i      the index of the parameter
     * @param scoped the scoped context
     * @return true if arguments may be consumed
     */
    public boolean mayConsumeArguments(int i, ContextArgumentStack scoped) {
        CommandContext context = scoped.getContext();
        ParameterData parameter = getParameters()[i];

        // Flag parameters: Always consume
        // Required non-flag parameters: Always consume
        // Optional non-flag parameters:
        //     - Before required parameters: Consume if there are 'left over' args
        //     - At the end: Always consumes

        if (parameter.isOptional()) {
            if (parameter.getFlag() != null) {
                return !parameter.isValueFlag() || context.hasFlag(parameter.getFlag());
            } else {
                int numberFree = context.argsLength() - scoped.position();
                for (int j = i; j < getParameters().length; j++) {
                    if (getParameters()[j].isNonFlagConsumer() && !getParameters()[j].isOptional()) {
                        // We already checked if the consumed count was > -1
                        // when we created this object
                        numberFree -= getParameters()[j].getConsumedCount();
                    }
                }

                // Skip this optional parameter
                if (numberFree < 1) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get the default value for a parameter.
     *
     * @param i      the index of the parameter
     * @param scoped the scoped context
     * @return a value
     * @throws ParameterException on an error
     * @throws CommandException   on an error
     */
    public Object getDefaultValue(int i, ContextArgumentStack scoped) throws ParameterException, CommandException, InvocationTargetException {
        CommandContext context = scoped.getContext();
        ParameterData parameter = getParameters()[i];

        String[] defaultValue = parameter.getDefaultValue();
        if (defaultValue != null) {
            try {
                return parameter.getBinding().bind(parameter, new StringArgumentStack(context, defaultValue, false), false);
            } catch (MissingParameterException e) {
                throw new ParametricException(
                        "The default value of the parameter using the binding " +
                                parameter.getBinding().getClass() + " in the method\n" +
                                toString() + "\nis invalid");
            }
        }

        return null;
    }


    /**
     * Check to see if all arguments, including flag arguments, were consumed.
     *
     * @param scoped the argument scope
     * @throws UnconsumedParameterException thrown if parameters were not consumed
     */
    public void checkUnconsumed(ContextArgumentStack scoped) throws UnconsumedParameterException {
        CommandContext context = scoped.getContext();
        String unconsumed;
        String unconsumedFlags = getUnusedFlags(context);

        if ((unconsumed = scoped.getUnconsumed()) != null) {
            throw new UnconsumedParameterException(unconsumed + " " + unconsumedFlags);
        }

        if (unconsumedFlags != null) {
            throw new UnconsumedParameterException(unconsumedFlags);
        }
    }

    /**
     * Get any unused flag arguments.
     *
     * @param context the command context
     */
    public String getUnusedFlags(CommandContext context) {
        if (!anyFlags()) {
            Set<Character> unusedFlags = null;
            for (char flag : context.getFlags()) {
                boolean found = false;

                if (getLegacyFlags().contains(flag)) {
                    break;
                }

                for (ParameterData parameter : getParameters()) {
                    Character paramFlag = parameter.getFlag();
                    if (paramFlag != null && flag == paramFlag) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    if (unusedFlags == null) {
                        unusedFlags = new HashSet<>();
                    }
                    unusedFlags.add(flag);
                }
            }

            if (unusedFlags != null) {
                StringBuilder builder = new StringBuilder();
                for (Character flag : unusedFlags) {
                    builder.append("-").append(flag).append(" ");
                }

                return builder.toString().trim();
            }
        }

        return null;
    }

    @Override
    public boolean testPermission(CommandLocals locals) {
        String[] perms = getPermissions();
        if (perms != null && perms.length != 0) {
            for (String perm : perms) {
                if (getBuilder().getAuthorizer().testPermission(locals, perm)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    @Override
    public List<String> getSuggestions(String arguments, CommandLocals locals) throws CommandException {
        String[] split = ("ignored" + " " + arguments).split(" ", -1);

        // &a<current> &f<next>
        // &cerrors

        CommandContext context = new CommandContext(split, getValueFlags(), !arguments.endsWith(" "), locals);
        ContextArgumentStack scoped = new ContextArgumentStack(context);
        SuggestionContext suggestable = context.getSuggestionContext();

        List<String> suggestions = new ArrayList<>(2);
        ParameterData parameter = null;
        ParameterData[] parameters = getParameters();
        String consumed = "";

        boolean hasSuggestion = false;
        int maxConsumedI = 0; // The maximum argument index
        int minConsumedI = 0; // The minimum argument that has been consumed
        // Collect parameters
        try {
            for (;maxConsumedI < parameters.length; maxConsumedI++) {
                parameter = parameters[maxConsumedI];
                if (parameter.getBinding().getBehavior(parameter) != BindingBehavior.PROVIDES) {
                    if (mayConsumeArguments(maxConsumedI, scoped)) {
                        // Parse the user input into a method argument
                        ArgumentStack usedArguments = getScopedContext(parameter, scoped);

                        usedArguments.mark();
                        try {
                            parameter.getBinding().bind(parameter, usedArguments, false);
                            minConsumedI = maxConsumedI + 1;
                        } catch (Throwable e) {
                            while (e.getCause() != null && !(e instanceof ParameterException || e instanceof InvocationTargetException))
                                e = e.getCause();
                            consumed = usedArguments.reset();
                            // Not optional? Then we can't execute this command
                            if (!parameter.isOptional()) {
                                if (!(e instanceof MissingParameterException)) minConsumedI = maxConsumedI;
                                throw e;
                            }
                        }
                    }
                }
            }
            if (minConsumedI >= maxConsumedI && (parameter == null || parameter.getType() == CommandContext.class)) checkUnconsumed(scoped);
        } catch (MissingParameterException ignore) {
        } catch (UnconsumedParameterException e) {
            suggestions.add(BBC.color("&cToo many parameters! Unused parameters: " + e.getUnconsumed()));
        } catch (ParameterException e) {
            String name = parameter.getName();
            suggestions.add(BBC.color("&cFor parameter '" + name + "': " + e.getMessage()));
        } catch (InvocationTargetException e) {
            SuggestInputParseException suggestion = SuggestInputParseException.get(e);
            if (suggestion != null && !suggestion.getSuggestions().isEmpty()) {
                hasSuggestion = true;
                suggestions.addAll(suggestion.getSuggestions());
            } else {
                Throwable t = e;
                while (t.getCause() != null) t = t.getCause();
                String msg = t.getMessage();
                String name = parameter.getName();
                if (msg != null && !msg.isEmpty()) suggestions.add(BBC.color("&cFor parameter '" + name + "': " + msg));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new WrappedCommandException(t);
        }
        // If there's 1 or less suggestions already, then show parameter suggestion
        if (!hasSuggestion && suggestions.size() <= 1) {
            StringBuilder suggestion = new StringBuilder();
            outer:
            for (String prefix = ""; minConsumedI < parameters.length; minConsumedI++) {
                parameter = parameters[minConsumedI];
                if (parameter.getBinding().getBehavior(parameter) != BindingBehavior.PROVIDES) {
                    suggestion.append(prefix);
                    List<String> argSuggestions = parameter.getBinding().getSuggestions(parameter, consumed);
                    switch (argSuggestions.size()) {
                        case 0:
                            break;
                        case 1:
                            suggestion.append(argSuggestions.iterator().next());
                            break;
                        default:
                            suggestion.setLength(0);
                            suggestions.addAll(argSuggestions);
                            break outer;

                    }
                    consumed = "";
                    prefix = " ";
                }
            }
            if (suggestion.length() != 0) suggestions.add(suggestion.toString());
        }
        return suggestions;
    }
}
