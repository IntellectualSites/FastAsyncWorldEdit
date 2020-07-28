package com.boydti.fawe.configuration;

/**
 * Various settings for controlling the input and output of a {@link Configuration}.
 */
public class ConfigurationOptions {
    private char pathSeparator = '.';
    private boolean copyDefaults = false;
    private final Configuration configuration;

    protected ConfigurationOptions(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the {@link Configuration} that this object is responsible for.
     *
     * @return Parent configuration
     */
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Gets the char that will be used to separate {@link ConfigurationSection}s.
     *
     * <p>
     * This value does not affect how the {@link Configuration} is stored,
     * only in how you access the data. The default value is '.'.
     * </p>
     *
     * @return Path separator
     */
    public char pathSeparator() {
        return pathSeparator;
    }

    /**
     * Sets the char that will be used to separate {@link ConfigurationSection}s.
     *
     * <p>
     * This value does not affect how the {@link Configuration} is stored,
     * only in how you access the data. The default value is '.'.
     * </p>
     *
     * @param value Path separator
     * @return This object, for chaining
     */
    public ConfigurationOptions pathSeparator(final char value) {
        pathSeparator = value;
        return this;
    }

    /**
     * Checks if the {@link Configuration} should copy values from its default
     * {@link Configuration} directly.
     *
     * <p>
     * If this is true, all values in the default Configuration will be
     * directly copied, making it impossible to distinguish between values
     * that were set and values that are provided by default. As a result,
     * {@link ConfigurationSection#contains(String)} will always
     * return the same value as {@link
     * ConfigurationSection#isSet(String)}. The default value is false.
     * </p>
     *
     * @return Whether or not defaults are directly copied
     */
    public boolean copyDefaults() {
        return copyDefaults;
    }

    /**
     * Sets if the {@link Configuration} should copy values from its default
     * {@link Configuration} directly.
     *
     * <p>
     * If this is true, all values in the default Configuration will be
     * directly copied, making it impossible to distinguish between values
     * that were set and values that are provided by default. As a result,
     * {@link ConfigurationSection#contains(String)} will always
     * return the same value as {@link
     * ConfigurationSection#isSet(String)}. The default value is false.
     * </p>
     *
     * @param value Whether or not defaults are directly copied
     * @return This object, for chaining
     */
    public ConfigurationOptions copyDefaults(final boolean value) {
        copyDefaults = value;
        return this;
    }
}
