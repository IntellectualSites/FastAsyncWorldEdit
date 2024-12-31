package com.fastasyncworldedit.core.extension.platform.binding;

import com.fastasyncworldedit.core.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.apache.logging.log4j.Logger;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Bindings {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final WorldEdit worldEdit;

    public Bindings(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public WorldEdit getWorldEdit() {
        return worldEdit;
    }

    public void register(InjectedValueStore store, CommandManager manager) {
        for (Method method : getClass().getDeclaredMethods()) {
            register(method, store, manager);
        }
    }

    private boolean register(Method method, InjectedValueStore store, CommandManager manager) {
        // Check that it has the binding
        Binding binding = method.getAnnotation(Binding.class);
        if (binding == null) {
            return false;
        }
        Annotation[] annotations = method.getAnnotations();

        // Get the key
        Class<?> ret = method.getReturnType();
        Key key;
        if (annotations.length == 1) {
            key = Key.of(ret);
        } else if (annotations.length == 2) {
            Annotation annotation = annotations[0] == binding ? annotations[1] : annotations[0];
            key = Key.of(ret, annotation);
        } else {
            LOGGER.error("Cannot annotate: {} with {}", method, StringMan.getString(annotations));
            return false;
        }

        // Get the provided parameters
        Class<?>[] params = method.getParameterTypes();
        Annotation[][] paramAnns = method.getParameterAnnotations();
        Function<InjectedValueAccess, Object>[] argsFunc = new Function[params.length];

        boolean provide = true;
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            if (param != String.class) {
                Annotation[] paramAnn = paramAnns[i];
                Key paramKey;
                if (paramAnn.length == 1) {
                    paramKey = Key.of(param, paramAnn[0]);
                } else if (paramAnn.length == 0) {
                    paramKey = Key.of(param);
                } else {
                    throw new UnsupportedOperationException("Only one annotation is permitted for " + method);
                }
                argsFunc[i] = v -> v.injectedValue(paramKey);
            } else if (provide) {
                provide = false;
            } else {
                throw new UnsupportedOperationException("Only one argument is allowed");
            }
        }

        // If the method provides all parameters
        if (provide) {
            store.injectValue(key, access -> Optional.ofNullable(invoke(null, argsFunc, access, method)));
        } else { // If the method consumes a String argument
            manager.registerConverter(key, new ArgumentConverter<Object>() {
                @Override
                public Component describeAcceptableArguments() {
                    return TextComponent.of(binding.value());
                }

                @Override
                public ConversionResult<Object> convert(String s, InjectedValueAccess access) {
                    try {
                        Object o = invoke(s, argsFunc, access, method);
                        if (o == null) {
                            return FailedConversion.from(new NullPointerException());
                        }
                        return SuccessfulConversion.fromSingle(o);
                    } catch (Throwable t) {
                        return FailedConversion.from(t);
                    }
                }
            });
        }
        return true;
    }

    private Object invoke(
            String arg,
            Function<InjectedValueAccess, Object>[] argsFunc,
            InjectedValueAccess access,
            Method method
    ) {
        try {
            Object[] args = new Object[argsFunc.length];
            for (int i = 0; i < argsFunc.length; i++) {
                Function<InjectedValueAccess, Object> func = argsFunc[i];
                if (func != null) {
                    Optional optional = (Optional) func.apply(access);
                    args[i] = optional.orElse(null);
                } else {
                    args[i] = arg;
                }
            }
            return method.invoke(this, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (!(e.getCause() instanceof StopExecutionException)) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

}
