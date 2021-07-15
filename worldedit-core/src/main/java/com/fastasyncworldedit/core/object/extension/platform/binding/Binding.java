package com.fastasyncworldedit.core.object.extension.platform.binding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Binding {
    String value() default "";
}
