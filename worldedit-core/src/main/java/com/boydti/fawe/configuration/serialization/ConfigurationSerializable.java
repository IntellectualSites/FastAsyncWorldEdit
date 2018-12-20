package com.boydti.fawe.configuration.serialization;

import java.util.Map;

/**
 * Represents an object that may be serialized.
 * <p>
 * These objects MUST implement one of the following, in addition to the
 * methods as defined by this interface:
 * <ul>
 * <li>A static method "deserialize" that accepts a single {@link java.util.Map}&lt;
 * {@link String}, {@link Object}> and returns the class.</li>
 * <li>A static method "valueOf" that accepts a single {@link java.util.Map}&lt;{@link
 * String}, {@link Object}> and returns the class.</li>
 * <li>A constructor that accepts a single {@link java.util.Map}&lt;{@link String},
 * {@link Object}>.</li>
 * </ul>
 * In addition to implementing this interface, you must register the class
 * with {@link com.boydti.fawe.configuration.serialization.ConfigurationSerialization#registerClass(Class)}.
 *
 * @see com.boydti.fawe.configuration.serialization.DelegateDeserialization
 * @see com.boydti.fawe.configuration.serialization.SerializableAs
 */
public interface ConfigurationSerializable {

    /**
     * Creates a Map representation of this class.
     * <p>
     * This class must provide a method to restore this class, as defined in
     * the {@link com.boydti.fawe.configuration.serialization.ConfigurationSerializable} interface javadoc.
     *
     * @return Map containing the current state of this class
     */
    Map<String, Object> serialize();
}
