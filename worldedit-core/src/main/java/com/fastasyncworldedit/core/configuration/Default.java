package com.fastasyncworldedit.core.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The value an option has until the configuration file sets another, written as it would be in the file:
 * {@code @Default("true")}, {@code @Default("50_000_000")}, {@code @Default("textures")}, or the name of the constant
 * for an enum option. A list option takes its entries, e.g. {@code @Default({"a", "b"})} or {@code @Default({})}, and a
 * {@code Map} of named groups takes the names of the groups that exist by default, each with the defaults of its own
 * options.
 * <p>
 * A value that does not suit its option is reported when the configuration is first used. An option whose default can
 * only be worked out at runtime has none. It is given its value where the defaults of the whole configuration are
 * created, such as {@link Settings#defaults()}.
 *
 * @since TODO
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Default {

    String[] value();

}
