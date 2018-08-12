package com.boydti.fawe.command;

import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.extension.input.InputParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SuggestInputParseException extends InputParseException {

    private final String message;
    private String prefix;
    private ArrayList<String> suggestions = new ArrayList<>();

    public SuggestInputParseException(String input, Collection<String> inputs) {
        super("");
        this.message = "Suggested input: " + StringMan.join(suggestions = getSuggestions(input, inputs), ", ");
        this.prefix = "";
    }

    public SuggestInputParseException(String input, String... inputs) {
        super("");
        this.message = "Suggested input: " + StringMan.join(suggestions = getSuggestions(input, inputs), ", ");
        this.prefix = "";
    }

    @Override
    public String getMessage() {
        return message;
    }

    public List<String> getSuggestions() {
        return MainUtil.prepend(prefix, suggestions);
    }

    public SuggestInputParseException prepend(String input) {
        this.prefix = input + prefix;
        return this;
    }

    public static SuggestInputParseException get(Throwable e) {
        if (e instanceof SuggestInputParseException) {
            return (SuggestInputParseException) e;
        }
        Throwable cause = e.getCause();
        if (cause == null) {
            return null;
        }
        return get(cause);
    }

    private static ArrayList<String> getSuggestions(String input, Collection<String> inputs) {
        ArrayList<String> suggestions = new ArrayList<>();
        if (input != null) {
            String tmp = input.toLowerCase();
            for (String s : inputs) {
                if (s.startsWith(tmp)) {
                    suggestions.add(s);
                }

            }
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(inputs);
        }
        return suggestions;
    }

    private static ArrayList<String> getSuggestions(String input, String... inputs) {
        ArrayList<String> suggestions = new ArrayList<>();
        if (input != null) {
            String tmp = input.toLowerCase();
            for (String s : inputs) {
                if (s.startsWith(tmp)) {
                    suggestions.add(s);
                }

            }
        }
        if (suggestions.isEmpty()) {
            for (String s : inputs) {
                suggestions.add(s);
            }
        }
        return suggestions;
    }
}
