package com.sk89q.worldedit.util.command.parametric;

import com.boydti.fawe.command.SuggestInputParseException;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.util.command.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                        unusedFlags = new HashSet<Character>();
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
        CommandContext context = new CommandContext(split, getValueFlags(), !arguments.endsWith(" "), locals);
        ContextArgumentStack scoped = new ContextArgumentStack(context);
        SuggestionContext suggestable = context.getSuggestionContext();


        // For /command -f |
        // For /command -f flag|
        if (suggestable.forFlag()) {
            for (int i = 0; i < getParameters().length; i++) {
                ParameterData parameter = getParameters()[i];

                if (parameter.getFlag() == suggestable.getFlag()) {
                    String prefix = context.getFlag(parameter.getFlag());
                    if (prefix == null) {
                        prefix = "";
                    }

//                    System.out.println("(0) Return get binding suggestions " + parameter + " | " + prefix);
                    return parameter.getBinding().getSuggestions(parameter, prefix);
                }
            }

            // This should not happen
//            System.out.println("(1) This should not happen");
            return new ArrayList<String>();
        }

        int consumerIndex = 0;
        ParameterData lastConsumer = null;
        String lastConsumed = null;

        for (int i = 0; i < getParameters().length; i++) {
            ParameterData parameter = getParameters()[i];
            if (parameter.getFlag() != null) {
                continue; // We already handled flags
            }
            try {
                scoped.mark();
                parameter.getBinding().bind(parameter, scoped, true);
                if (scoped.wasConsumed()) {
                    lastConsumer = parameter;
                    lastConsumed = context.getString(scoped.position() - 1);
                    consumerIndex++;
                }
            } catch (MissingParameterException e) {
                // For /command value1 |value2
                // For /command |value1 value2
                if (suggestable.forHangingValue()) {
//                    System.out.println("(2) Return get binding dangling " + parameter + " | " + "");
                    return parameter.getBinding().getSuggestions(parameter, "");
                } else {
                    // For /command value1| value2
                    if (lastConsumer != null) {
//                        System.out.println("(3) Return get consumed " + lastConsumer + " | " + lastConsumed);
                        return lastConsumer.getBinding().getSuggestions(lastConsumer, lastConsumed);
                        // For /command| value1 value2
                        // This should never occur
                    } else {
//                        System.out.println("(4) Invalid suggestion context");
                        throw new RuntimeException("Invalid suggestion context");
                    }
                }
            } catch (ParameterException | InvocationTargetException e) {
                SuggestInputParseException suggestion = SuggestInputParseException.get(e);
                if (suggestion != null) {
//                    System.out.println("(5) Has suggestion " + suggestion.getSuggestions());
                    return suggestion.getSuggestions();
                }
                if (suggestable.forHangingValue()) {
                    String name = getDescription().getParameters().get(consumerIndex).getName();
//                    System.out.println("(6) Has dangling invalid " + name + " | " + e.getMessage());
                    throw new InvalidUsageException("For parameter '" + name + "': " + e.getMessage(), this);
                } else {
//                    System.out.println("(7) HGet binding suggestions " + parameter + " | " + lastConsumed);
                    return parameter.getBinding().getSuggestions(parameter, "");
                }
            }
        }
        // For /command value1 value2 |
        if (suggestable.forHangingValue()) {
            // There's nothing that we can suggest because there's no more parameters
            // to add on, and we can't change the previous parameter
//            System.out.println("(7.1) No more parameters");
            return new ArrayList<String>();
        } else {
            // For /command value1 value2|
            if (lastConsumer != null) {
//                System.out.println("(8) Get binding suggestions " + lastConsumer + " | " + lastConsumed);
                return lastConsumer.getBinding().getSuggestions(lastConsumer, lastConsumed);
                // This should never occur
            } else {
//                System.out.println("(9) Invalid suggestion context");
                throw new RuntimeException("Invalid suggestion context");
            }

        }
    }
}
