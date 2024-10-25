package com.sk89q.worldedit.command.util.annotation;

import org.enginehub.piston.inject.InjectAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates it is expected that blocks will only be set synchronously, i.e. from one thread (at a time)
 *
 * @since 2.12.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.METHOD
})
@InjectAnnotation
public @interface SynchronousSettingExpected {

}
