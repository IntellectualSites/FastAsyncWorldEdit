package com.fastasyncworldedit.core.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a section of a {@link Configuration}.
 */
public interface ConfigurationSection {

    /**
     * Gets a set containing all keys in this section.
     *
     * <p>
     * If deep is set to true, then this will contain all the keys within any child {@link
     * ConfigurationSection}s (and their children, etc). These will be in a valid path notation for
     * you to use.
     * </p>
     *
     * <p>
     * If deep is set to false, then this will contain only the keys of any direct children, and not
     * their own children.
     * </p>
     *
     * @param deep Whether or not to get a deep list, as opposed to a shallow list.
     * @return Set of keys contained within this ConfigurationSection.
     */
    Set<String> getKeys(boolean deep);

    /**
     * Gets a Map containing all keys, and their values for this section.
     *
     * <p>
     * If deep is set to true, then this will contain all the keys and values within any child
     * {@link ConfigurationSection}s (and their children, etc). These keys will be in a valid path
     * notation for you to use.
     * </p>
     *
     * <p>
     * If deep is set to false, then this will contain only the keys and values of any direct
     * children, and not their own children.
     * </p>
     *
     * @param deep Whether or not to get a deep list, as opposed to a shallow list.
     * @return Map of keys and values of this section.
     */
    Map<String, Object> getValues(boolean deep);

    /**
     * Checks if this {@link ConfigurationSection} contains the given path.
     *
     * <p>
     * If the value for the requested path does not exist, but a default value has been specified,
     * this will return true.
     * </p>
     *
     * @param path Path to check for existence.
     * @return True if this section contains the requested path, either via default or being set.
     * @throws IllegalArgumentException Thrown when path is null.
     */
    boolean contains(String path);

    /**
     * Checks if this {@link ConfigurationSection} has a value set for the given path.
     *
     * <p>
     * If the value for the requested path does not exist, but a default value has been specified,
     * this will still return false.
     * </p>
     *
     * @param path Path to check for existence.
     * @return True if this section contains the requested path, regardless of having a default.
     * @throws IllegalArgumentException Thrown when path is null.
     */
    boolean isSet(String path);

    /**
     * Gets the path of this {@link ConfigurationSection} from its root {@link Configuration}.
     *
     * <p>
     * For any {@link Configuration} themselves, this will return an empty string.
     * </p>
     *
     * <p>
     * If the section is no longer contained within its root for any reason, such as being replaced
     * with a different value, this may return null.
     * </p>
     *
     * <p>
     * To retrieve the single name of this section, that is, the final part of the path returned by
     * this method, you may use {@link #getName()}.
     * </p>
     *
     * @return Path of this section relative to its root
     */
    String getCurrentPath();

    /**
     * Gets the name of this individual {@link ConfigurationSection}, in the path.
     *
     * <p>
     * This will always be the final part of {@link #getCurrentPath()}, unless the section is
     * orphaned.
     * </p>
     *
     * @return Name of this section
     */
    String getName();

    /**
     * Gets the root {@link Configuration} that contains this {@link ConfigurationSection}.
     *
     * <p>
     * For any {@link Configuration} themselves, this will return its own object.
     * </p>
     *
     * <p>
     * If the section is no longer contained within its root for any reason, such as being replaced
     * with a different value, this may return null.
     * </p>
     *
     * @return Root configuration containing this section.
     */
    Configuration getRoot();

    /**
     * Gets the parent {@link ConfigurationSection} that directly contains this {@link
     * ConfigurationSection}.
     * <p>
     * For any {@link Configuration} themselves, this will return null.
     * </p>
     *
     * <p>
     * If the section is no longer contained within its parent for any reason, such as being
     * replaced with a different value, this may return null.
     * </p>
     *
     * @return Parent section containing this section.
     */
    ConfigurationSection getParent();

    /**
     * Gets the requested Object by path.
     *
     * <p>
     * If the Object does not exist, but a default value has been specified, this will return the
     * default value. If the Object does not exist and no default value was specified, this will
     * return null.
     * </p>
     *
     * @param path Path of the Object to get.
     * @return Requested Object.
     */
    Object get(String path);

    /**
     * Gets the requested Object by a path, returning a default value if not found.
     *
     * <p>
     * If the Object does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the Object to get.
     * @param def  The default value to return if the path is not found.
     * @return Requested Object.
     */
    Object get(String path, Object def);

    /**
     * Sets the specified path to the given value.
     *
     * <p>
     * If value is null, the entry will be removed. Any existing entry will be replaced, regardless
     * of what the new value is.
     * </p>
     *
     * <p>
     * Some implementations may have limitations on what you may store. See their individual javadoc
     * for details. No implementations should allow you to store {@link Configuration}s or {@link
     * ConfigurationSection}s, please use {@link #createSection(String)} for that.
     * </p>
     *
     * @param path  Path of the object to set.
     * @param value New value to set the path to.
     */
    void set(String path, Object value);

    /**
     * Creates an empty {@link ConfigurationSection} at the specified path.
     *
     * <p>
     * Any value that was previously set at this path will be overwritten. If the previous value was
     * itself a {@link ConfigurationSection}, it will be orphaned.
     * </p>
     *
     * @param path Path to create the section at.
     * @return Newly created section
     */
    ConfigurationSection createSection(String path);

    /**
     * Creates a {@link ConfigurationSection} at the specified path, with specified values.
     *
     * <p>
     * Any value that was previously set at this path will be overwritten. If the previous value was
     * itself a {@link ConfigurationSection}, it will be orphaned.
     * </p>
     *
     * @param path Path to create the section at.
     * @param map  The values to used.
     * @return Newly created section
     */
    ConfigurationSection createSection(String path, Map<?, ?> map);

    // Primitives

    /**
     * Gets the requested String by a path.
     *
     * <p>
     * If the String does not exist, but a default value has been specified, this will return the
     * default value. If the String does not exist and no default value was specified, this will
     * return null.
     * </p>
     *
     * @param path Path of the String to get.
     * @return Requested String.
     */
    String getString(String path);

    /**
     * Gets the requested String by a path, returning a default value if not
     * found.
     *
     * <p>
     * If the String does not exist then the specified default value will
     * return regardless of if a default has been identified in the root
     * {@link Configuration}.
     * </p>
     *
     * @param path Path of the String to get.
     * @param def  The default value to return if the path is not found or is
     *             not a String.
     * @return Requested String.
     */
    String getString(String path, String def);

    /**
     * Checks if the specified path is a String.
     *
     * <p>
     * If the path exists but is not a String, this will return false. If the path does not exist,
     * this will return false. If the path does not exist, but a default value has been specified,
     * this will check if that default value is a String and return appropriately.
     * </p>
     *
     * @param path Path of the String to check.
     * @return Whether or not the specified path is a String.
     */
    boolean isString(String path);

    /**
     * Gets the requested int by a path.
     *
     * <p>
     * If the int does not exist, but a default value has been specified, this
     * will return the default value. If the int does not exist and no default
     * value was specified, this will return 0.
     * </p>
     *
     * @param path Path of the int to get.
     * @return Requested int.
     */
    int getInt(String path);

    /**
     * Gets the requested int by a path, returning a default value if not found.
     *
     * <p>
     * If the int does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the int to get.
     * @param def  The default value to return if the path is not found or is not an int.
     * @return Requested int.
     */
    int getInt(String path, int def);

    /**
     * Checks if the specified path is an int.
     *
     * <p>
     * If the path exists but is not an int, this will return false. If the path does not exist,
     * this will return false. If the path does not exist, but a default value has been specified,
     * this will check if that default value is an int and return appropriately
     * </p>
     *
     * @param path Path of the int to check.
     * @return Whether or not the specified path is an int.
     */
    boolean isInt(String path);

    /**
     * Gets the requested boolean by a path.
     *
     * <p>
     * If the boolean does not exist, but a default value has been specified, this will return the
     * default value. If the boolean does not exist and no default value was specified, this will
     * return false.
     * </p>
     *
     * @param path Path of the boolean to get.
     * @return Requested boolean.
     */
    boolean getBoolean(String path);

    /**
     * Gets the requested boolean by a path, returning a default value if not found.
     *
     * <p>
     * If the boolean does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the boolean to get.
     * @param def  The default value to return if the path is not found or is not a boolean.
     * @return Requested boolean.
     */
    boolean getBoolean(String path, boolean def);

    /**
     * Checks if the specified path is a boolean.
     *
     * <p>
     * If the path exists but is not a boolean, this will return false. If the path does not exist,
     * this will return false. If the path does not exist, but a default value has been specified,
     * this will check if that default value is a boolean and return appropriately.
     * </p>
     *
     * @param path Path of the boolean to check.
     * @return Whether or not the specified path is a boolean.
     */
    boolean isBoolean(String path);

    /**
     * Gets the requested double by a path.
     *
     * <p>
     * If the double does not exist, but a default value has been specified, this will return the
     * default value. If the double does not exist and no default value was specified, this will
     * return 0.
     * </p>
     *
     * @param path Path of the double to get.
     * @return Requested double.
     */
    double getDouble(String path);

    /**
     * Gets the requested double by a path, returning a default value if not found.
     *
     * <p>
     * If the double does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the double to get.
     * @param def  The default value to return if the path is not found or is not a double.
     * @return Requested double.
     */
    double getDouble(String path, double def);

    /**
     * Checks if the specified path is a double.
     *
     * <p>
     * If the path exists but is not a double, this will return false. If the path does not exist,
     * this will return false. If the path does not exist, but a default value has been specified,
     * this will check if that default value is a double and return appropriately.
     * </p>
     *
     * @param path Path of the double to check.
     * @return Whether or not the specified path is a double.
     */
    boolean isDouble(String path);

    /**
     * Gets the requested long by a path.
     *
     * <p>
     * If the long does not exist, but a default value has been specified, this will return the
     * default value. If the long does not exist and no default value was specified, this will
     * return 0.
     * </p>
     *
     * @param path Path of the long to get.
     * @return Requested long.
     */
    long getLong(String path);

    /**
     * Gets the requested long by a path, returning a default value if not found.
     *
     * <p>
     * If the long does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the long to get.
     * @param def  The default value to return if the path is not found or is not a long.
     * @return Requested long.
     */
    long getLong(String path, long def);

    /**
     * Checks if the specified path is a long.
     *
     * <p>
     * If the path exists but is not a long, this will return false. If the path does not exist,
     * this will return false. If the path does not exist, but a default value has been specified,
     * this will check if that default value is a long and return appropriately.
     * </p>
     *
     * @param path Path of the long to check.
     * @return Whether or not the specified path is a long.
     */
    boolean isLong(String path);

    // Java

    /**
     * Gets the requested List by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return null.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List.
     */
    List<?> getList(String path);

    /**
     * Gets the requested List by a path, returning a default value if not found.
     *
     * <p>
     * If the List does not exist then the specified default value will return regardless of if a
     * default has been identified in the root {@link Configuration}.
     * </p>
     *
     * @param path Path of the List to get.
     * @param def  The default value to return if the path is not found or is not a List.
     * @return Requested List.
     */
    List<?> getList(String path, List<?> def);

    /**
     * Checks if the specified path is a List.
     *
     * <p>
     * If the path exists but is not a List, this will return false. If the
     * path does not exist, this will return false. If the path does not exist, but
     * a default value has been specified, this will check if that default
     * value is a List and return appropriately.
     * </p>
     *
     * @param path Path of the List to check.
     * @return Whether or not the specified path is a List.
     */
    boolean isList(String path);

    /**
     * Gets the requested List of String by path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this
     * will return the default value. If the List does not exist and no
     * default value was specified, this will return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a String if possible,
     * but may miss any values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of String.
     */
    List<String> getStringList(String path);

    /**
     * Gets the requested List of an Integer by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into an Integer if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Integer.
     */
    List<Integer> getIntegerList(String path);

    /**
     * Gets the requested List of Boolean by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Boolean if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Boolean.
     */
    List<Boolean> getBooleanList(String path);

    /**
     * Gets the requested List of Double by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Double if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Double.
     */
    List<Double> getDoubleList(String path);

    /**
     * Gets the requested List of Float by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Float if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Float.
     */
    List<Float> getFloatList(String path);

    /**
     * Gets the requested List of Long by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Long if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Long.
     */
    List<Long> getLongList(String path);

    /**
     * Gets the requested List of Byte by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Byte if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Byte.
     */
    List<Byte> getByteList(String path);

    /**
     * Gets the requested List of Character by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Character if possible, but may miss any
     * values out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Character.
     */
    List<Character> getCharacterList(String path);

    /**
     * Gets the requested List of Short by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Short if possible, but may miss any values
     * out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Short.
     */
    List<Short> getShortList(String path);

    /**
     * Gets the requested List of Maps by a path.
     *
     * <p>
     * If the List does not exist, but a default value has been specified, this will return the
     * default value. If the List does not exist and no default value was specified, this will
     * return an empty List.
     * </p>
     *
     * <p>
     * This method will attempt to cast any values into a Map if possible, but may miss any values
     * out if they are not compatible.
     * </p>
     *
     * @param path Path of the List to get.
     * @return Requested List of Maps.
     */
    List<Map<?, ?>> getMapList(String path);

    /**
     * Gets the requested ConfigurationSection by path.
     *
     * <p>
     * If the ConfigurationSection does not exist, but a default value has been specified, this will
     * return the default value. If the ConfigurationSection does not exist and no default value was
     * specified, this will return null.
     * </p>
     *
     * @param path Path of the ConfigurationSection to get.
     * @return Requested ConfigurationSection.
     */
    ConfigurationSection getConfigurationSection(String path);

    /**
     * Checks if the specified path is a {@link ConfigurationSection}.
     *
     * <p>
     * If the path exists but is not a {@code ConfigurationSection}, this will return false. If the
     * path does not exist, this will return false. If the path does not exist, but a default value
     * has been specified, this will check if that default value is a ConfigurationSection and
     * return appropriately.
     * </p>
     *
     * @param path Path of the ConfigurationSection to check.
     * @return Whether or not the specified path is a ConfigurationSection.
     */
    boolean isConfigurationSection(String path);

    /**
     * Gets the equivalent {@link ConfigurationSection} from the default
     * {@link Configuration} defined in {@link #getRoot()}.
     *
     * <p>
     * If the root contains no defaults, or the defaults doesn't contain a
     * value for this path, or the value at this path is not a {@link
     * ConfigurationSection} then this will return null.
     * </p>
     *
     * @return Equivalent section in root configuration
     */
    ConfigurationSection getDefaultSection();

    /**
     * Sets the default value in the root at the given path as provided.
     *
     * <p>
     * If no source {@link Configuration} was provided as a default collection, then a new {@link
     * MemoryConfiguration} will be created to hold the new default value.
     * </p>
     *
     * <p>
     * If value is null, the value will be removed from the default Configuration source.
     * </p>
     *
     * <p>
     * If the value as returned by {@link #getDefaultSection()} is null, then this will create a new
     * section at the path, replacing anything that may have existed there previously.
     * </p>
     *
     * @param path  Path of the value to set.
     * @param value Value to set the default to.
     * @throws IllegalArgumentException Thrown if path is null.
     */
    void addDefault(String path, Object value);

}
