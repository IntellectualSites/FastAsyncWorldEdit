package com.thevoxelbox.voxelsniper.jsap;

import com.martiansoftware.jsap.ParseException;
import com.martiansoftware.jsap.StringParser;

/**
 * A {@link com.martiansoftware.jsap.StringParser} for parsing Integers. The parse() method delegates the actual parsing to Integer.decode(String).
 *
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 * @see com.martiansoftware.jsap.StringParser
 * @see Integer
 */
public class NullableIntegerStringParser extends StringParser {

    @SuppressWarnings("unused")
    private static final NullableIntegerStringParser INSTANCE = new NullableIntegerStringParser();

    /**
     * Returns a {@link com.thevoxelbox.voxelsniper.jsap.NullableIntegerStringParser}.
     * <p>
     * <p>
     * Convenient access to the only instance returned by this method is available through {@link com.martiansoftware.jsap.JSAP#INTEGER_PARSER}.
     *
     * @return a {@link com.thevoxelbox.voxelsniper.jsap.NullableIntegerStringParser}.
     */
    public static NullableIntegerStringParser getParser() {
        return new NullableIntegerStringParser();
    }

    /**
     * Creates a new IntegerStringParser.
     *
     * @deprecated Use {@link #getParser()} or, even better, {@link com.martiansoftware.jsap.JSAP#INTEGER_PARSER}.
     */
    public NullableIntegerStringParser() {
        super();
    }

    /**
     * Parses the specified argument into an Integer. This method delegates the parsing to <code>Integer.decode(arg)</code>. If <code>Integer.decode()</code>
     * throws a NumberFormatException, it is encapsulated into a ParseException and re-thrown.
     *
     * @param arg the argument to parse
     * @return an Integer object with the value contained in the specified argument.
     * @throws com.martiansoftware.jsap.ParseException if <code>Integer.decode(arg)</code> throws a NumberFormatException.
     * @see Integer
     * @see com.martiansoftware.jsap.StringParser#parse(String)
     */
    @Override
    public final Object parse(final String arg) throws ParseException {
        if (arg == null) {
            return null;
        }

        Integer result;
        try {
            result = Integer.decode(arg);
        } catch (NumberFormatException nfe) {
            throw (new ParseException("Unable to convert '" + arg + "' to an Integer.", nfe));
        }
        return (result);
    }
}
