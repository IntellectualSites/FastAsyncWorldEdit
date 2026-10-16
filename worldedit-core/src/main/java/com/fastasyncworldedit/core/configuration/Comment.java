package com.fastasyncworldedit.core.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lines written above an option in the configuration file, or above a section when placed on the section's record.
 *
 * @since TODO
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE})
public @interface Comment {

    String[] value();

}
