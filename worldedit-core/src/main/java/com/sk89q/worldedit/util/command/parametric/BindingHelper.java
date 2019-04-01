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

package com.sk89q.worldedit.util.command.parametric;

import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Validate;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
@Deprecated
public class BindingHelper implements Binding {
    
    private final List<BindingMap.BoundMethod> bindings;
    private final Type[] types;
    
    /**
     * Create a new instance.
     */
    public BindingHelper() {
        List<BindingMap.BoundMethod> bindings = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        
        for (Method method : this.getClass().getMethods()) {
            BindingMatch info = method.getAnnotation(BindingMatch.class);
            if (info != null) {
                Class<? extends Annotation> classifier = null;
                
                // Set classifier
                if (!info.classifier().equals(Annotation.class)) {
                    classifier = info.classifier();
                    types.add(classifier);
                }
                
                for (Type t : info.type()) {
                    Type type = null;
                    
                    // Set type
                    if (!t.equals(Class.class)) {
                        type = t;
                        if (classifier == null) {
                            types.add(type); // Only if there is no classifier set!
                        }
                    }
                    
                    // Check to see if at least one is set
                    if (type == null && classifier == null) {
                        throw new RuntimeException(
                                "A @BindingMatch needs either a type or classifier set");
                    }
                    
                    BindingMap.BoundMethod handler = new BindingMap.BoundMethod(info, type, classifier, method, this);
                    bindings.add(handler);
                }
            }
        }
        
        Collections.sort(bindings);
        
        this.bindings = bindings;
        
        Type[] typesArray = new Type[types.size()];
        types.toArray(typesArray);
        this.types = typesArray;
        
    }
    
    /**
     * Match a {@link BindingMatch} according to the given parameter.
     * 
     * @param parameter the parameter
     * @return a binding
     */
    private BindingMap.BoundMethod match(ParameterData parameter) {
        for (BindingMap.BoundMethod binding : bindings) {
            Annotation classifer = parameter.getClassifier();
            Type type = parameter.getType();
            
            if (binding.classifier != null) {
                if (classifer != null && classifer.annotationType().equals(binding.classifier)) {
                    if (binding.type == null || binding.type.equals(type)) {
                        return binding;
                    }
                }
            } else if (binding.type.equals(type)) {
                return binding;
            }
        }
        
        throw new RuntimeException("Unknown type");
    }

    @Override
    public Type[] getTypes() {
        return types;
    }

    @Override
    public int getConsumedCount(ParameterData parameter) {
        return match(parameter).annotation.consumedCount();
    }

    @Override
    public BindingBehavior getBehavior(ParameterData parameter) {
        return match(parameter).annotation.behavior();
    }

    @Override
    public Object bind(ParameterData parameter, ArgumentStack scoped,
            boolean onlyConsume) throws ParameterException, CommandException, InvocationTargetException {
        BindingMap.BoundMethod binding = match(parameter);
        List<Object> args = new ArrayList<>();
        args.add(scoped);
        
        if (binding.classifier != null) {
            args.add(parameter.getClassifier());
        }
        
        if (binding.annotation.provideModifiers()) {
            args.add(parameter.getModifiers());
        }
        
        if (onlyConsume && binding.annotation.behavior() == BindingBehavior.PROVIDES) {
            return null; // Nothing to consume, nothing to do
        }
        
        Object[] argsArray = new Object[args.size()];
        args.toArray(argsArray);
        
        try {
            return binding.method.invoke(this, argsArray);
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

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    public static void validate(double number, Annotation[] modifiers)
            throws ParameterException {
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

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    public static void validate(int number, Annotation[] modifiers)
            throws ParameterException {
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

    /**
     * Validate a string value using relevant modifiers.
     *
     * @param string the string
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    public static void validate(String string, Annotation[] modifiers)
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
                                        "The given text doesn't match the right " +
                                                "format (technically speaking, the 'format' is %s)",
                                        validate.regex()));
                    }
                }
            }
        }
    }
}
