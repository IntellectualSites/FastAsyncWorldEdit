package com.sk89q.worldedit.extension.platform.binding;

import static org.slf4j.LoggerFactory.getLogger;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.util.ValueProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Bindings {

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
        if (binding == null) return false;
        Annotation[] annotations = method.getAnnotations();

        // Get the key
        Class<?> ret = method.getReturnType();
        Key key;
        if ( annotations.length == 1) {
            key = Key.of(ret);
        }else if (annotations.length == 2) {
            Annotation annotation = annotations[0] == binding ? annotations[1] : annotations[0];
            key = Key.of(ret, annotation);
        } else {
            getLogger(Bindings.class).debug("Cannot annotate: " + method + " with " + StringMan.getString(annotations));
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
            store.injectValue(key, access -> Optional.of(invoke(null, argsFunc, access, method)));
        } else { // If the method consumes a String argument
            manager.registerConverter(key, new ArgumentConverter<Object>() {
                @Override
                public Component describeAcceptableArguments() {
                    return TextComponent.of(binding.value());
                }

                @Override
                public ConversionResult<Object> convert(String s, InjectedValueAccess access) {
                    return SuccessfulConversion.fromSingle(invoke(s, argsFunc, access, method));
                }
            });
        }
        return true;
    }

    private Object invoke(String arg, Function<InjectedValueAccess, Object>[] argsFunc, InjectedValueAccess access, Method method) {
        try {
            Object[] args = new Object[argsFunc.length];
            for (int i = 0; i < argsFunc.length; i++) {
                Function<InjectedValueAccess, Object> func = argsFunc[i];
                if (func != null) {
                    Optional optional = (Optional) func.apply(access);
                    args[i] = optional.get();
                } else {
                    args[i] = arg;
                }
            }
            return method.invoke(this, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
