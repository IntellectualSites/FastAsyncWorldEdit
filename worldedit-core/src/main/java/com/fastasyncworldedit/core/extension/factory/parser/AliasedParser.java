package com.fastasyncworldedit.core.extension.factory.parser;

import java.util.List;

public interface AliasedParser {

    /**
     * The strings this parser matches.
     *
     * @return the matching aliases
     */
    List<String> getMatchedAliases();

}
