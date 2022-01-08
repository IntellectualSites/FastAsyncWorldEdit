package com.sk89q.worldedit.command.util.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Link {

    //FAWE start - address rawtypes
    Class<?> clazz() default Link.class;
    //FAWE end

    String value();

}
