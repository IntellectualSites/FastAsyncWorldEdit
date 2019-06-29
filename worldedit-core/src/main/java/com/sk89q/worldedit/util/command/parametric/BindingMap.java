package com.sk89q.worldedit.util.command.parametric;

import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.MissingParameterException;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.binding.Range;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A binding helper that uses the {@link BindingMatch} annotation to make
 * writing bindings extremely easy.
 *
 * <p>Methods must have the following and only the following parameters:</p>
 *
 * <ul>
 *   <li>A {@link ArgumentStack}</li>
 *   <li>A {@link Annotation} <strong>if there is a classifier set</strong></li>
 *   <li>A {@link Annotation}[]
 *       <strong>if there {@link BindingMatch#provideModifiers()} is true</strong></li>
 * </ul>
 *
 * <p>Methods may throw any exception. Exceptions may be converted using a
 * {@link ExceptionConverter} registered with the {@link ParametricBuilder}.</p>
 */
public class BindingMap implements Binding {

    private final Set<Type> types;
    private final Map<Type, Binding> legacy;
    private final Map<Type, List<BoundMethod>> bindings;
    private final Map<Type, SimpleDispatcher> dynamicBindings;
    private final ParametricBuilder builder;

    /**
     * Create a new instance.
     * @param builder
     */
    public BindingMap(ParametricBuilder builder) {
        this.dynamicBindings = new HashMap<>();
        this.legacy = new HashMap<>();
        this.bindings = new HashMap<>();
        this.builder = builder;
        this.types = new HashSet<>();

    }

    public void add(Object object, Type... requiredTypes) {
        Method[] methods = object.getClass().getDeclaredMethods();
        for (Method method : methods) {
            method.setAccessible(true);
            BindingMatch info = method.getAnnotation(BindingMatch.class);
            if (info != null) {
                Class<? extends Annotation> classifier = null;

                // Set classifier
                if (!info.classifier().equals(Annotation.class)) {
                    classifier = info.classifier();
                }

                for (Type type : info.type()) {
                    if (type == Void.class) {
                        type = method.getReturnType();
                    }
                    BoundMethod handler = new BoundMethod(info, type, classifier, method, object);
                    List<BoundMethod> list = bindings.get(type);
                    if (list == null) bindings.put(type, list = new ArrayList<>());
                    list.add(handler);
                    types.add(type);
                }
            }
            Command definition = method.getAnnotation(Command.class);
            Class<?> type = method.getReturnType();
            if (definition != null && type != null) {
                SimpleDispatcher dispatcher = dynamicBindings.get(type);
                if (dispatcher == null) dynamicBindings.put(type, dispatcher = new SimpleDispatcher());
                builder.registerMethodAsCommands(method, dispatcher, object, null);
                types.add(type);
            }
        }
        if (requiredTypes != null && requiredTypes.length > 0) {
            for (Type type : requiredTypes) {
                legacy.put(type, (Binding) object);
            }
        }
    }

    /**
     * Match a {@link BindingMatch} according to the given parameter.
     *
     * @param pd the parameter
     * @return a binding
     */
    private BoundMethod match(ParameterData pd) {
        Type type = pd.getType();
        BoundMethod result = null;
        while (type != null) {
            List<BoundMethod> methods = bindings.get(type);
            if (methods != null) {
                for (BoundMethod binding : methods) {
                    if (binding.classifier != null) {
                        if (pd.getClassifier() != null && pd.getClassifier().annotationType().equals(binding.classifier)) {
                            if (binding.type == null) {
                                result = binding;
                            } else if (binding.type.equals(type)) {
                                return binding;
                            }

                        }
                    } else if (binding.type.equals(type)) {
                        if (result == null) result = binding;
                    }
                }
            }
            if (result != null) return result;
            type = (type instanceof Class) ? ((Class) type).getSuperclass() : null;
        }
        throw new RuntimeException("Unknown type " + pd.getType());
    }

    private SimpleDispatcher matchDynamic(ParameterData pd) {
        return dynamicBindings.get(pd.getType());
    }

    @Override
    public int getConsumedCount(ParameterData parameter) {
        return match(parameter).annotation.consumedCount();
    }

    @Override
    public BindingBehavior getBehavior(ParameterData parameter) {
        BoundMethod matched = match(parameter);
        if (matched != null) return matched.annotation.behavior();
        SimpleDispatcher dynamic = matchDynamic(parameter);
        return dynamic != null ? BindingBehavior.CONSUMES : null;
    }

    @Override
    public Type[] getTypes() {
        return types.toArray(new Type[0]);
    }

    @Override
    public Object bind(ParameterData parameter, ArgumentStack scoped, boolean onlyConsume) throws ParameterException, CommandException, InvocationTargetException {
        BoundMethod binding = match(parameter);
        List<Object> args = new ArrayList<>();
        args.add(scoped);

        if (binding.classifier != null) {
            args.add(parameter.getClassifier());
        }

        if (binding.annotation.provideModifiers()) {
            args.add(parameter.getModifiers());
        }

        if (binding.annotation.provideType()) {
            args.add(parameter.getType());
        }

        if (onlyConsume && binding.annotation.behavior() == BindingBehavior.PROVIDES) {
            return null; // Nothing to consume, nothing to do
        }

        if (binding.annotation.behavior() != BindingBehavior.PROVIDES) {
            SimpleDispatcher dynamic = matchDynamic(parameter);
            if (dynamic != null) {
                scoped.mark();
                String rest = scoped.remaining();
                scoped.reset();
                int start = rest.indexOf('{');
                if (start > 0) {
                    int end = StringMan.findMatchingBracket(rest, start);
                    if (end > start) {
                        String alias = rest.substring(0, start);
                        CommandMapping cmd = dynamic.get(alias);
                        if (cmd != null) {
                            String arguments = rest.substring(start + 1, end);
                            CommandLocals locals = scoped.getContext().getLocals();
                            Object result = cmd.getCallable().call(arguments, locals, new String[0]);
                            int remaining = rest.length() - end;
                            while (rest.length() > remaining) {
                                scoped.next();
                                try {
                                    scoped.mark();
                                    rest = scoped.remaining();
                                    scoped.reset();
                                } catch (MissingParameterException ignore) { rest = ""; }
                            }
                            return result;
                        }
                    }
                }
            }
        }
        Object[] argsArray = new Object[args.size()];
        args.toArray(argsArray);

        try {
            return binding.method.invoke(binding.object, argsArray);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Processing of classifier " + parameter.getClassifier() +
                            " and type " + parameter.getType() + " failed for method\n" +
                            binding.method + "\nbecause the parameters for that method are wrong", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ParameterException) {
                throw (ParameterException) e.getCause();
            } else if (e.getCause() instanceof CommandException) {
                throw (CommandException) e.getCause();
            }
            throw e;
        }
    }

    @Override
    public List<String> getSuggestions(ParameterData parameter, String prefix) {
        Binding legacySuggestions = legacy.get(parameter.getType());
        if (legacySuggestions != null) {
            List<String> result = legacySuggestions.getSuggestions(parameter, prefix);
            if (result != null) return result;
        }
        if (prefix.isEmpty()) {
            char bracket = parameter.isOptional() ? '[' : '<';
            char endBracket = StringMan.getMatchingBracket(bracket);
            StringBuilder result = new StringBuilder();
            result.append("\u00A75");
            result.append(bracket);
            result.append("\u00A7r");
            if (parameter.getFlag() != null) {
                result.append('-').append(parameter.getFlag()).append("\u00A75 \u00A7r");
            }
            result.append(parameter.getName());
            if (parameter.getDefaultValue() != null) {
                result.append('=').append(StringMan.join(parameter.getDefaultValue(), " "));
            }
            Range range = parameter.getModifier(Range.class);
            if (range != null) {
                result.append('|').append(StringMan.prettyFormat(range.min())).append(",").append(StringMan.prettyFormat(range.max()));
            }
            result.append("\u00A75");
            result.append(endBracket);
            result.append("\u00A7r");
            return Collections.singletonList(result.toString());
        }
        return new ArrayList<>();
    }

    protected static class BoundMethod implements Comparable<BoundMethod> {
        protected final BindingMatch annotation;
        protected final Type type;
        protected final Class<? extends Annotation> classifier;
        protected final Method method;
        protected final Object object;

        BoundMethod(BindingMatch annotation, Type type,
                    Class<? extends Annotation> classifier, Method method, Object object) {
            this.annotation = annotation;
            this.type = type;
            this.classifier = classifier;
            this.method = method;
            this.object = object;
        }

        @Override
        public int compareTo(BoundMethod o) {
            if (classifier != null && o.classifier == null) {
                return -1;
            } else if (classifier == null && o.classifier != null) {
                return 1;
            } else if (classifier != null && o.classifier != null) {
                if (type != null && o.type == null) {
                    return -1;
                } else if (type == null && o.type != null) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }
}

