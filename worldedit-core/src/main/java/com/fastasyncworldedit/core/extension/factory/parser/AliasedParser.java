package com.fastasyncworldedit.core.extension.factory.parser;

import java.util.Collection;

public interface AliasedParser {

    /**
     * The strings this parser matches.
     *
     * @return the matching aliases
     */
    Collection<String> getMatchedAliases();

}
