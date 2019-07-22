package com.sk89q.worldedit.extension.platform.binding;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.internal.annotation.Validate;

import java.lang.annotation.Annotation;

public class AnnotatedBindings extends Bindings {

    private final WorldEdit worldEdit;

    public AnnotatedBindings(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    @Validate()
    public String getText(String argument, Validate modifier) {
        return validate(argument, modifier);
    }

    /**
     * Validate a string value using relevant modifiers.
     *
     * @param string the string
     * @param modifiers the list of modifiers to scan
     * @throws InputParseException on a validation error
     */
    private static String validate(String string, Annotation... modifiers) {
        if (string != null) {
            for (Annotation modifier : modifiers) {
                if (modifier instanceof Validate) {
                    Validate validate = (Validate) modifier;

                    if (!validate.value().isEmpty()) {
                        if (!string.matches(validate.value())) {
                            throw new InputParseException(
                                    String.format(
                                            "The given text doesn't match the right format (technically speaking, the 'format' is %s)",
                                            validate.value()));
                        }
                    }
                }
            }
        }
        return string;
    }
}
