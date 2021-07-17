package com.fastasyncworldedit.core.util.annotation;

import org.enginehub.piston.inject.InjectAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a {@code List<BlockState>} parameter to inject a list of BlockStates.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@InjectAnnotation
public @interface PatternList {
}
