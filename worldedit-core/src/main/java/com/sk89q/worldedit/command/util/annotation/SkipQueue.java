package com.sk89q.worldedit.command.util.annotation;

import com.sk89q.worldedit.command.util.CommandQueuedConditionGenerator;
import org.enginehub.piston.annotation.CommandCondition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Sets a command to be queued
 */
@Retention(RetentionPolicy.RUNTIME)
@CommandCondition(CommandQueuedConditionGenerator.class)
public @interface SkipQueue {
    boolean value() default true;
}
