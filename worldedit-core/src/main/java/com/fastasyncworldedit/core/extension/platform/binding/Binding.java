package com.fastasyncworldedit.core.extension.platform.binding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Binding {

    String value() default "";

}
